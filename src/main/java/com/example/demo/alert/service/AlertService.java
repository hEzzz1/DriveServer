package com.example.demo.alert.service;

import com.example.demo.alert.dto.AlertActionLogItemData;
import com.example.demo.alert.dto.AlertActionLogsResponseData;
import com.example.demo.alert.dto.AlertOperationResponseData;
import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.entity.AlertActionLog;
import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.event.AlertRealtimeEvent;
import com.example.demo.alert.model.AlertActionType;
import com.example.demo.alert.model.AlertStatus;
import com.example.demo.alert.repository.AlertActionLogRepository;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AlertService {

    private static final DateTimeFormatter ALERT_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AlertEventRepository alertEventRepository;
    private final AlertActionLogRepository alertActionLogRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AlertService(AlertEventRepository alertEventRepository,
                        AlertActionLogRepository alertActionLogRepository,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.alertEventRepository = alertEventRepository;
        this.alertActionLogRepository = alertActionLogRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public AlertOperationResponseData createAlert(CreateAlertRequest request, AuthenticatedUser operator) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String normalizedRemark = normalizeRemark(request.getRemark());

        AlertEvent alert = new AlertEvent();
        alert.setAlertNo(generateAlertNo(now));
        alert.setFleetId(request.getFleetId());
        alert.setVehicleId(request.getVehicleId());
        alert.setDriverId(request.getDriverId());
        alert.setRuleId(request.getRuleId());
        alert.setRiskLevel(request.getRiskLevel().byteValue());
        alert.setRiskScore(request.getRiskScore());
        alert.setFatigueScore(request.getFatigueScore());
        alert.setDistractionScore(request.getDistractionScore());
        alert.setTriggerTime(LocalDateTime.ofInstant(request.getTriggerTime().toInstant(), ZoneOffset.UTC));
        alert.setStatus(AlertStatus.NEW.getCode());
        alert.setLatestActionBy(operator.getUserId());
        alert.setLatestActionTime(now);
        alert.setRemark(normalizedRemark);
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        AlertEvent saved = alertEventRepository.save(alert);
        saveActionLog(saved.getId(), AlertActionType.CREATE, operator.getUserId(), now, normalizedRemark);
        applicationEventPublisher.publishEvent(AlertRealtimeEvent.created(saved));
        return toOperationResponse(saved, AlertActionType.CREATE);
    }

    @Transactional
    public AlertOperationResponseData confirmAlert(Long alertId, String remark, AuthenticatedUser operator) {
        return transition(alertId, AlertStatus.CONFIRMED, AlertActionType.CONFIRM, remark, operator);
    }

    @Transactional
    public AlertOperationResponseData markFalsePositive(Long alertId, String remark, AuthenticatedUser operator) {
        return transition(alertId, AlertStatus.FALSE_POSITIVE, AlertActionType.FALSE_POSITIVE, remark, operator);
    }

    @Transactional
    public AlertOperationResponseData closeAlert(Long alertId, String remark, AuthenticatedUser operator) {
        return transition(alertId, AlertStatus.CLOSED, AlertActionType.CLOSE, remark, operator);
    }

    @Transactional(readOnly = true)
    public AlertActionLogsResponseData listActionLogs(Long alertId) {
        if (!alertEventRepository.existsById(alertId)) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        List<AlertActionLogItemData> items = alertActionLogRepository.findByAlertIdOrderByActionTimeAscIdAsc(alertId).stream()
                .map(log -> new AlertActionLogItemData(
                        log.getActionType(),
                        log.getActionBy(),
                        toOffsetDateTime(log.getActionTime()),
                        log.getActionRemark()))
                .toList();
        return new AlertActionLogsResponseData(alertId, items);
    }

    private AlertOperationResponseData transition(Long alertId,
                                                  AlertStatus targetStatus,
                                                  AlertActionType actionType,
                                                  String remark,
                                                  AuthenticatedUser operator) {
        AlertEvent alert = getAlertOrThrow(alertId);
        AlertStatus currentStatus = AlertStatus.fromCode(alert.getStatus());
        if (!canTransit(currentStatus, targetStatus)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前状态不允许执行" + actionType.name() + "操作");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String normalizedRemark = normalizeRemark(remark);

        alert.setStatus(targetStatus.getCode());
        alert.setLatestActionBy(operator.getUserId());
        alert.setLatestActionTime(now);
        alert.setRemark(normalizedRemark);
        alert.setUpdatedAt(now);

        AlertEvent saved = alertEventRepository.save(alert);
        saveActionLog(saved.getId(), actionType, operator.getUserId(), now, normalizedRemark);
        applicationEventPublisher.publishEvent(AlertRealtimeEvent.updated(saved, actionType));
        return toOperationResponse(saved, actionType);
    }

    private AlertEvent getAlertOrThrow(Long alertId) {
        return alertEventRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private boolean canTransit(AlertStatus currentStatus, AlertStatus targetStatus) {
        if (currentStatus == AlertStatus.NEW) {
            return targetStatus == AlertStatus.CONFIRMED
                    || targetStatus == AlertStatus.FALSE_POSITIVE
                    || targetStatus == AlertStatus.CLOSED;
        }
        if (currentStatus == AlertStatus.CONFIRMED) {
            return targetStatus == AlertStatus.FALSE_POSITIVE
                    || targetStatus == AlertStatus.CLOSED;
        }
        return false;
    }

    private AlertOperationResponseData toOperationResponse(AlertEvent alert, AlertActionType actionType) {
        return new AlertOperationResponseData(
                alert.getId(),
                alert.getAlertNo(),
                (int) alert.getStatus(),
                alert.getLatestActionBy(),
                toOffsetDateTime(alert.getLatestActionTime()),
                actionType.name());
    }

    private void saveActionLog(Long alertId,
                               AlertActionType actionType,
                               Long actionBy,
                               LocalDateTime actionTime,
                               String remark) {
        AlertActionLog log = new AlertActionLog();
        log.setAlertId(alertId);
        log.setActionType(actionType.name());
        log.setActionBy(actionBy);
        log.setActionTime(actionTime);
        log.setActionRemark(remark);
        log.setCreatedAt(actionTime);
        alertActionLogRepository.save(log);
    }

    private String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        return remark.trim();
    }

    private String generateAlertNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ALT" + now.format(ALERT_NO_TIME_FORMATTER) + random;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atOffset(ZoneOffset.UTC);
    }
}
