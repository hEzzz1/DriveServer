package com.example.demo.ingest.service;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.ingest.dto.IngestEventResponseData;
import com.example.demo.ingest.idempotency.EventIdempotencyStore;
import com.example.demo.ingest.stream.EventStreamPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;

@Service
public class IngestService {

    private static final String INVALID_DEVICE_TOKEN_MSG = "设备token无效";

    private final EventIdempotencyStore eventIdempotencyStore;
    private final EventStreamPublisher eventStreamPublisher;
    private final Duration eventIdTtl;
    private final Set<String> allowedDeviceTokens;

    public IngestService(EventIdempotencyStore eventIdempotencyStore,
                         EventStreamPublisher eventStreamPublisher,
                         @Value("${ingest.idempotency.ttl:24h}") Duration eventIdTtl,
                         @Value("${ingest.security.device-tokens:dev-device-token}") String allowedDeviceTokens) {
        this.eventIdempotencyStore = eventIdempotencyStore;
        this.eventStreamPublisher = eventStreamPublisher;
        this.eventIdTtl = eventIdTtl;
        this.allowedDeviceTokens = StringUtils.commaDelimitedListToSet(allowedDeviceTokens).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (this.allowedDeviceTokens.isEmpty()) {
            throw new IllegalStateException("ingest.security.device-tokens must not be empty");
        }
    }

    public IngestEventResponseData ingest(String deviceToken, IngestEventRequest request) {
        validateDeviceToken(deviceToken);

        String eventId = request.getEventId() == null ? null : request.getEventId().trim();
        if (!StringUtils.hasText(eventId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage());
        }
        request.setEventId(eventId);

        boolean acquired = eventIdempotencyStore.tryAcquire(eventId, eventIdTtl);
        if (!acquired) {
            throw new BusinessException(ApiCode.IDEMPOTENT_CONFLICT, ApiCode.IDEMPOTENT_CONFLICT.getMessage());
        }

        try {
            eventStreamPublisher.publish(request);
        } catch (RuntimeException ex) {
            eventIdempotencyStore.release(eventId);
            throw ex;
        }

        return new IngestEventResponseData(true);
    }

    private void validateDeviceToken(String deviceToken) {
        if (!StringUtils.hasText(deviceToken)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, INVALID_DEVICE_TOKEN_MSG);
        }
        String normalized = deviceToken.trim();
        if (!allowedDeviceTokens.contains(normalized)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, INVALID_DEVICE_TOKEN_MSG);
        }
    }
}
