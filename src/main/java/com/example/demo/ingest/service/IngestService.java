package com.example.demo.ingest.service;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.service.DeviceService;
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

import java.time.Duration;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final EventIdempotencyStore eventIdempotencyStore;
    private final EventStreamPublisher eventStreamPublisher;
    private final EventAlertOrchestrator eventAlertOrchestrator;
    private final DeviceService deviceService;
    private final DrivingSessionService drivingSessionService;
    private final Duration eventIdTtl;

    public IngestService(EventIdempotencyStore eventIdempotencyStore,
                         EventStreamPublisher eventStreamPublisher,
                         EventAlertOrchestrator eventAlertOrchestrator,
                         DeviceService deviceService,
                         DrivingSessionService drivingSessionService,
                         @Value("${ingest.idempotency.ttl:24h}") Duration eventIdTtl) {
        this.eventIdempotencyStore = eventIdempotencyStore;
        this.eventStreamPublisher = eventStreamPublisher;
        this.eventAlertOrchestrator = eventAlertOrchestrator;
        this.deviceService = deviceService;
        this.drivingSessionService = drivingSessionService;
        this.eventIdTtl = eventIdTtl;
    }

    public IngestEventResponseData ingest(String deviceCode, String deviceToken, IngestEventRequest request) {
        Device device = deviceService.authenticate(deviceCode, deviceToken).device();

        String eventId = request.getEventId() == null ? null : request.getEventId().trim();
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage());
        }
        request.setEventId(eventId);

        boolean acquired = eventIdempotencyStore.tryAcquire(eventId, eventIdTtl);
        if (!acquired) {
            log.warn("INGEST_WARNING_DUPLICATE eventId={} vehicleId={}", eventId, request.getVehicleId());
            throw new BusinessException(ApiCode.IDEMPOTENT_CONFLICT, ApiCode.IDEMPOTENT_CONFLICT.getMessage());
        }

        try {
            EventOwnershipResolution resolution = drivingSessionService.resolveOwnership(
                    device,
                    parseOptionalBusinessId(request.getFleetId()),
                    parseOptionalBusinessId(request.getVehicleId()),
                    parseOptionalBusinessId(request.getDriverId()),
                    request.getSessionId());
            eventStreamPublisher.publish(request);
            eventAlertOrchestrator.process(request, device, resolution);
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
}
