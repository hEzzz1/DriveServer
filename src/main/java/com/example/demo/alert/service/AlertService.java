package com.example.demo.alert.service;

import com.example.demo.alert.dto.AlertActionLogItemData;
import com.example.demo.alert.dto.AlertActionLogsResponseData;
import com.example.demo.alert.dto.AlertDetailResponseData;
import com.example.demo.alert.dto.AlertListItemData;
import com.example.demo.alert.dto.AlertOperationResponseData;
import com.example.demo.alert.dto.AlertPageResponseData;
import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.entity.AlertActionLog;
import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.model.AlertActionType;
import com.example.demo.alert.model.AlertStatus;
import com.example.demo.alert.repository.AlertActionLogRepository;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.auth.service.BusinessDataScope;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.realtime.dto.RealtimeAlertMessageData;
import com.example.demo.realtime.listener.RealtimeAlertBroadcastEvent;
import com.example.demo.rule.entity.RuleConfig;
import com.example.demo.rule.repository.RuleConfigRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class AlertService {

    private static final DateTimeFormatter ALERT_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String INVALID_TRANSITION_MESSAGE = "当前状态不允许该操作";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int EVIDENCE_RETENTION_DAYS = 30;

    private final AlertEventRepository alertEventRepository;
    private final AlertActionLogRepository alertActionLogRepository;
    private final SystemAuditService systemAuditService;
    private final BusinessAccessService businessAccessService;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DeviceRepository deviceRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AlertService(AlertEventRepository alertEventRepository,
                        AlertActionLogRepository alertActionLogRepository,
                        SystemAuditService systemAuditService,
                        BusinessAccessService businessAccessService,
                        FleetRepository fleetRepository,
                        VehicleRepository vehicleRepository,
                        DriverRepository driverRepository,
                        DeviceRepository deviceRepository,
                        RuleConfigRepository ruleConfigRepository,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.alertEventRepository = alertEventRepository;
        this.alertActionLogRepository = alertActionLogRepository;
        this.systemAuditService = systemAuditService;
        this.businessAccessService = businessAccessService;
        this.fleetRepository = fleetRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.deviceRepository = deviceRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public AlertOperationResponseData createAlert(CreateAlertRequest request, AuthenticatedUser operator) {
        if (operator.getSubjectType() == SubjectType.USER) {
            businessAccessService.assertCanAccessData(operator, request.getEnterpriseId(), request.getFleetId());
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String normalizedRemark = normalizeRemark(request.getRemark());

        AlertEvent alert = new AlertEvent();
        alert.setAlertNo(generateAlertNo(now, request.getDriverId()));
        alert.setEnterpriseId(request.getEnterpriseId());
        alert.setFleetId(request.getFleetId());
        alert.setVehicleId(request.getVehicleId());
        alert.setDriverId(request.getDriverId());
        alert.setDeviceId(request.getDeviceId());
        alert.setSessionId(request.getSessionId());
        alert.setReportedEnterpriseId(request.getReportedEnterpriseId());
        alert.setReportedFleetId(request.getReportedFleetId());
        alert.setReportedVehicleId(request.getReportedVehicleId());
        alert.setReportedDriverId(request.getReportedDriverId());
        alert.setResolvedEnterpriseId(request.getResolvedEnterpriseId());
        alert.setResolvedFleetId(request.getResolvedFleetId());
        alert.setResolvedVehicleId(request.getResolvedVehicleId());
        alert.setResolvedDriverId(request.getResolvedDriverId());
        alert.setResolutionStatus(normalizeOptionalText(request.getResolutionStatus()));
        alert.setConfigVersion(normalizeOptionalText(request.getConfigVersion()));
        alert.setRuleId(request.getRuleId());
        alert.setRiskLevel(request.getRiskLevel().byteValue());
        alert.setRiskScore(request.getRiskScore());
        alert.setFatigueScore(request.getFatigueScore());
        alert.setDistractionScore(request.getDistractionScore());
        alert.setEdgeRiskLevel(normalizeOptionalText(request.getEdgeRiskLevel()));
        alert.setEdgeDominantRiskType(normalizeOptionalText(request.getEdgeDominantRiskType()));
        alert.setEdgeTriggerReasons(normalizeOptionalText(request.getEdgeTriggerReasons()));
        alert.setEdgeWindowStartMs(request.getEdgeWindowStartMs());
        alert.setEdgeWindowEndMs(request.getEdgeWindowEndMs());
        alert.setEdgeCreatedAtMs(request.getEdgeCreatedAtMs());
        alert.setEvidenceType(normalizeOptionalText(request.getEvidenceType()));
        alert.setEvidenceUrl(normalizeOptionalText(request.getEvidenceUrl()));
        alert.setEvidenceMimeType(normalizeOptionalText(request.getEvidenceMimeType()));
        alert.setEvidenceCapturedAtMs(request.getEvidenceCapturedAtMs());
        alert.setEvidenceRetentionUntil(resolveEvidenceRetentionUntil(request.getEvidenceUrl(), request.getEvidenceRetentionUntil(), now));
        alert.setTriggerTime(LocalDateTime.ofInstant(request.getTriggerTime().toInstant(), ZoneOffset.UTC));
        alert.setStatus(AlertStatus.NEW.getCode());
        alert.setLatestActionBy(operator.getUserId());
        alert.setLatestActionTime(now);
        alert.setRemark(normalizedRemark);
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        AlertEvent saved = alertEventRepository.save(alert);
        saveActionLog(saved.getId(), AlertActionType.CREATE, operator.getUserId(), now, normalizedRemark);
        systemAuditService.record(operator, "ALERT", "CREATE_ALERT", "ALERT", saved.getId().toString(), "SUCCESS",
                normalizedRemark, java.util.Map.of(
                        "alertId", saved.getId(),
                        "alertNo", saved.getAlertNo(),
                        "ruleId", saved.getRuleId(),
                        "status", saved.getStatus()));
        publishRealtimeAlert("ALERT_CREATED", saved);
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
    public AlertActionLogsResponseData listActionLogs(Long alertId, AuthenticatedUser operator) {
        AlertEvent alert = getAlertOrThrow(alertId);
        businessAccessService.assertCanAccessData(operator, alert.getEnterpriseId(), alert.getFleetId());
        List<AlertActionLogItemData> items = alertActionLogRepository.findByAlertIdOrderByActionTimeAscIdAsc(alertId).stream()
                .map(log -> new AlertActionLogItemData(
                        log.getId(),
                        log.getActionType(),
                        log.getActionBy(),
                        toOffsetDateTime(log.getActionTime()),
                        log.getActionRemark()))
                .toList();
        return new AlertActionLogsResponseData(alertId, items);
    }

    @Transactional(readOnly = true)
    public AlertPageResponseData listAlerts(Integer page,
                                            Integer size,
                                            AuthenticatedUser operator,
                                            Long fleetId,
                                            Long vehicleId,
                                            Long driverId,
                                            Integer riskLevel,
                                            Integer status,
                                            OffsetDateTime startTime,
                                            OffsetDateTime endTime) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        LocalDateTime startTimeUtc = toUtcLocalDateTime(startTime);
        LocalDateTime endTimeUtc = toUtcLocalDateTime(endTime);
        if (startTimeUtc != null && endTimeUtc != null && startTimeUtc.isAfter(endTimeUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }

        BusinessDataScope dataScope = businessAccessService.resolveDataScope(operator, null, fleetId);
        Specification<AlertEvent> specification = buildListSpecification(
                dataScope, vehicleId, driverId, riskLevel, status, startTimeUtc, endTimeUtc);
        PageRequest pageable = PageRequest.of(
                pageNo - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "triggerTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<AlertEvent> pageResult = alertEventRepository.findAll(specification, pageable);
        AlertViewRefs refs = loadViewRefs(pageResult.getContent());
        List<AlertListItemData> items = pageResult.getContent().stream()
                .map(alert -> toListItemData(alert, refs))
                .toList();
        return new AlertPageResponseData(pageResult.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public AlertDetailResponseData getAlertDetail(Long alertId, AuthenticatedUser operator) {
        AlertEvent alert = getAlertOrThrow(alertId);
        businessAccessService.assertCanAccessData(operator, alert.getEnterpriseId(), alert.getFleetId());
        AlertViewRefs refs = loadViewRefs(List.of(alert));
        return new AlertDetailResponseData(
                alert.getId(),
                alert.getAlertNo(),
                alert.getFleetId(),
                fleetLabel(refs, alert.getFleetId()),
                alert.getVehicleId(),
                vehicleLabel(refs, alert.getVehicleId()),
                alert.getDriverId(),
                driverNameLabel(refs, alert.getDriverId()),
                driverCodeLabel(refs, alert.getDriverId()),
                alert.getDeviceId(),
                deviceCodeLabel(refs, alert.getDeviceId()),
                alert.getRuleId(),
                ruleNameLabel(refs, alert.getRuleId()),
                alert.getRiskLevel() == null ? null : (int) alert.getRiskLevel(),
                alert.getRiskScore(),
                alert.getFatigueScore(),
                alert.getDistractionScore(),
                toOffsetDateTime(alert.getTriggerTime()),
                alert.getStatus() == null ? null : (int) alert.getStatus(),
                alert.getLatestActionBy(),
                toOffsetDateTime(alert.getLatestActionTime()),
                alert.getRemark(),
                alert.getEdgeRiskLevel(),
                alert.getEdgeDominantRiskType(),
                alert.getEdgeTriggerReasons(),
                alert.getEdgeWindowStartMs(),
                alert.getEdgeWindowEndMs(),
                alert.getEdgeCreatedAtMs(),
                alert.getEvidenceType(),
                alert.getEvidenceUrl(),
                alert.getEvidenceMimeType(),
                alert.getEvidenceCapturedAtMs(),
                toOffsetDateTime(alert.getEvidenceRetentionUntil()));
    }

    private AlertOperationResponseData transition(Long alertId,
                                                  AlertStatus targetStatus,
                                                  AlertActionType actionType,
                                                  String remark,
                                                  AuthenticatedUser operator) {
        AlertEvent alert = getAlertOrThrow(alertId);
        businessAccessService.assertCanAccessData(operator, alert.getEnterpriseId(), alert.getFleetId());
        AlertStatus currentStatus = AlertStatus.fromCode(alert.getStatus());
        if (!canTransit(currentStatus, targetStatus)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, INVALID_TRANSITION_MESSAGE);
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
        systemAuditService.record(operator, "ALERT", actionType.name() + "_ALERT", "ALERT", saved.getId().toString(), "SUCCESS",
                normalizedRemark, java.util.Map.of(
                        "alertId", saved.getId(),
                        "alertNo", saved.getAlertNo(),
                        "fromStatus", currentStatus == null ? null : currentStatus.name(),
                        "toStatus", targetStatus.name()));
        publishRealtimeAlert("ALERT_UPDATED", saved);
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
        return normalizeOptionalText(remark);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String generateAlertNo(LocalDateTime now, Long driverId) {
        return "ALT" + now.format(ALERT_NO_TIME_FORMATTER) + driverId;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atOffset(ZoneOffset.UTC);
    }

    private AlertListItemData toListItemData(AlertEvent alert, AlertViewRefs refs) {
        return new AlertListItemData(
                alert.getId(),
                alert.getAlertNo(),
                alert.getFleetId(),
                fleetLabel(refs, alert.getFleetId()),
                alert.getVehicleId(),
                vehicleLabel(refs, alert.getVehicleId()),
                alert.getDriverId(),
                driverNameLabel(refs, alert.getDriverId()),
                driverCodeLabel(refs, alert.getDriverId()),
                alert.getDeviceId(),
                deviceCodeLabel(refs, alert.getDeviceId()),
                alert.getRuleId(),
                ruleNameLabel(refs, alert.getRuleId()),
                alert.getRiskLevel() == null ? null : (int) alert.getRiskLevel(),
                alert.getFatigueScore(),
                alert.getDistractionScore(),
                alert.getStatus() == null ? null : (int) alert.getStatus(),
                toOffsetDateTime(alert.getTriggerTime()),
                alert.getEvidenceType(),
                alert.getEvidenceUrl(),
                alert.getEvidenceMimeType(),
                alert.getEvidenceCapturedAtMs(),
                toOffsetDateTime(alert.getEvidenceRetentionUntil()));
    }

    private void publishRealtimeAlert(String eventType, AlertEvent alert) {
        AlertListItemData payload = toListItemData(alert, loadViewRefs(List.of(alert)));
        applicationEventPublisher.publishEvent(new RealtimeAlertBroadcastEvent(
                alert.getEnterpriseId(),
                alert.getFleetId(),
                new RealtimeAlertMessageData(eventType, OffsetDateTime.now(ZoneOffset.UTC), payload)));
    }

    private AlertViewRefs loadViewRefs(Collection<AlertEvent> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return new AlertViewRefs(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        return new AlertViewRefs(
                indexById(fleetRepository.findAllById(alerts.stream().map(AlertEvent::getFleetId).filter(id -> id != null).distinct().toList()), Fleet::getId),
                indexById(vehicleRepository.findAllById(alerts.stream().map(AlertEvent::getVehicleId).filter(id -> id != null).distinct().toList()), Vehicle::getId),
                indexById(driverRepository.findAllById(alerts.stream().map(AlertEvent::getDriverId).filter(id -> id != null).distinct().toList()), Driver::getId),
                indexById(deviceRepository.findAllById(alerts.stream().map(AlertEvent::getDeviceId).filter(id -> id != null).distinct().toList()), Device::getId),
                indexById(ruleConfigRepository.findAllById(alerts.stream().map(AlertEvent::getRuleId).filter(id -> id != null).distinct().toList()), RuleConfig::getId));
    }

    private <T> Map<Long, T> indexById(Iterable<T> items, Function<T, Long> idGetter) {
        Map<Long, T> result = new HashMap<>();
        for (T item : items) {
            Long id = idGetter.apply(item);
            if (id != null) {
                result.put(id, item);
            }
        }
        return result;
    }

    private String fleetLabel(AlertViewRefs refs, Long fleetId) {
        Fleet fleet = refs.fleetsById().get(fleetId);
        return fleet == null ? null : fleet.getName();
    }

    private String vehicleLabel(AlertViewRefs refs, Long vehicleId) {
        Vehicle vehicle = refs.vehiclesById().get(vehicleId);
        return vehicle == null ? null : vehicle.getPlateNumber();
    }

    private String driverNameLabel(AlertViewRefs refs, Long driverId) {
        Driver driver = refs.driversById().get(driverId);
        return driver == null ? null : driver.getName();
    }

    private String driverCodeLabel(AlertViewRefs refs, Long driverId) {
        Driver driver = refs.driversById().get(driverId);
        return driver == null ? null : driver.getDriverCode();
    }

    private String deviceCodeLabel(AlertViewRefs refs, Long deviceId) {
        Device device = refs.devicesById().get(deviceId);
        return device == null ? null : device.getDeviceCode();
    }

    private String ruleNameLabel(AlertViewRefs refs, Long ruleId) {
        RuleConfig rule = refs.rulesById().get(ruleId);
        return rule == null ? null : rule.getRuleName();
    }

    private LocalDateTime resolveEvidenceRetentionUntil(String evidenceUrl,
                                                        OffsetDateTime requestedRetentionUntil,
                                                        LocalDateTime now) {
        LocalDateTime requested = toUtcLocalDateTime(requestedRetentionUntil);
        if (requested != null) {
            return requested;
        }
        return StringUtils.hasText(evidenceUrl) ? now.plusDays(EVIDENCE_RETENTION_DAYS) : null;
    }

    private Specification<AlertEvent> buildListSpecification(BusinessDataScope dataScope,
                                                             Long vehicleId,
                                                             Long driverId,
                                                             Integer riskLevel,
                                                             Integer status,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
        List<Specification<AlertEvent>> specifications = new ArrayList<>();
        specifications.add((root, query, cb) -> dataScope.toPredicate(root, cb, "enterpriseId", "fleetId"));
        if (vehicleId != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("vehicleId"), vehicleId));
        }
        if (driverId != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("driverId"), driverId));
        }
        if (riskLevel != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("riskLevel"), riskLevel.byteValue()));
        }
        if (status != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("status"), status.byteValue()));
        }
        if (startTime != null) {
            specifications.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("triggerTime"), startTime));
        }
        if (endTime != null) {
            specifications.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("triggerTime"), endTime));
        }
        return specifications.stream().reduce(Specification.where(null), Specification::and);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "page必须大于等于1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "size必须大于等于1");
        }
        return Math.min(size, MAX_SIZE);
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
    }

    private record AlertViewRefs(
            Map<Long, Fleet> fleetsById,
            Map<Long, Vehicle> vehiclesById,
            Map<Long, Driver> driversById,
            Map<Long, Device> devicesById,
            Map<Long, RuleConfig> rulesById
    ) {
    }
}
