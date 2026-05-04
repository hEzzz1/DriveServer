package com.example.demo.ingest.service;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.service.DeviceService;
import com.example.demo.alert.service.AlertEvidenceService;
import com.example.demo.ingest.dto.IngestEvidenceResponseData;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.ingest.dto.IngestEventResponseData;
import com.example.demo.ingest.idempotency.EventIdempotencyStore;
import com.example.demo.session.service.DrivingSessionService;
import com.example.demo.session.service.EventOwnershipResolution;
import com.example.demo.ingest.stream.EventStreamPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final EventIdempotencyStore eventIdempotencyStore;
    private final EventStreamPublisher eventStreamPublisher;
    private final EventAlertOrchestrator eventAlertOrchestrator;
    private final DeviceService deviceService;
    private final DrivingSessionService drivingSessionService;
    private final AlertEvidenceService alertEvidenceService;
    private final Duration eventIdTtl;

    public IngestService(EventIdempotencyStore eventIdempotencyStore,
                         EventStreamPublisher eventStreamPublisher,
                         EventAlertOrchestrator eventAlertOrchestrator,
                         DeviceService deviceService,
                         DrivingSessionService drivingSessionService,
                         AlertEvidenceService alertEvidenceService,
                         @Value("${ingest.idempotency.ttl:24h}") Duration eventIdTtl) {
        this.eventIdempotencyStore = eventIdempotencyStore;
        this.eventStreamPublisher = eventStreamPublisher;
        this.eventAlertOrchestrator = eventAlertOrchestrator;
        this.deviceService = deviceService;
        this.drivingSessionService = drivingSessionService;
        this.alertEvidenceService = alertEvidenceService;
        this.eventIdTtl = eventIdTtl;
    }

    public IngestEventResponseData ingest(String deviceCode, String deviceToken, IngestEventRequest request) {
        Device device = deviceService.authenticateAndTouch(deviceCode, deviceToken).device();
        if (StringUtils.hasText(request.getDeviceCode()) && !device.getDeviceCode().equals(request.getDeviceCode().trim())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "deviceCode不匹配");
        }

        String eventId = request.getEventId() == null ? null : request.getEventId().trim();
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage());
        }
        request.setEventId(eventId);
        request.setDeviceCode(device.getDeviceCode());

        boolean acquired = eventIdempotencyStore.tryAcquire(eventId, eventIdTtl);
        if (!acquired) {
            log.warn("INGEST_WARNING_DUPLICATE eventId={} vehicleId={}", eventId, request.getVehicleId());
            throw new BusinessException(ApiCode.IDEMPOTENT_CONFLICT, ApiCode.IDEMPOTENT_CONFLICT.getMessage());
        }

        try {
            EventOwnershipResolution resolution = drivingSessionService.resolveOwnership(
                    device,
                    parseOptionalBusinessId(request.getReportedEnterpriseId()),
                    parseOptionalBusinessId(request.getFleetId()),
                    parseOptionalBusinessId(request.getVehicleId()),
                    parseOptionalBusinessId(request.getDriverId()),
                    request.getSessionId());
            eventStreamPublisher.publish(request);
            eventAlertOrchestrator.process(request, device, resolution);
            deviceService.recordUploadSuccess(device.getDeviceCode(), deviceToken, request.getEventTime());
            log.info("INGEST_WARNING_ACCEPTED eventId={} vehicleId={} fleetId={} driverId={} resolutionStatus={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    request.getFleetId(),
                    request.getDriverId(),
                    resolution.resolutionStatus());
        } catch (RuntimeException ex) {
            eventIdempotencyStore.release(eventId);
            throw ex;
        }

        return new IngestEventResponseData(true);
    }

    public IngestEvidenceResponseData uploadEvidence(String deviceCode,
                                                     String deviceToken,
                                                     String eventId,
                                                     String evidenceType,
                                                     String evidenceMimeType,
                                                     Long evidenceCapturedAtMs,
                                                     MultipartFile file) {
        deviceService.authenticateAndTouch(deviceCode, deviceToken);
        String normalizedEventId = normalizeEventId(eventId);
        AlertEvidenceService.StoredEvidence stored = alertEvidenceService.storeUploadedEvidence(
                normalizedEventId,
                evidenceType,
                evidenceMimeType,
                evidenceCapturedAtMs,
                file);
        return new IngestEvidenceResponseData(
                normalizedEventId,
                stored.evidenceType(),
                stored.evidenceUrl(),
                stored.evidenceMimeType(),
                stored.evidenceCapturedAtMs());
    }

    private Long parseOptionalBusinessId(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(trimmed);
        }
        throw new BusinessException(ApiCode.INVALID_PARAM, "业务ID格式非法");
    }

    private String normalizeEventId(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage());
        }
        return eventId.trim();
    }
}
