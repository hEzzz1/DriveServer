package com.example.demo.rule.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertCooldownDeduplicator {

    private static final Duration MINUTE_BUCKET_TTL = Duration.ofMinutes(2);

    private final Map<String, Instant> minuteBucketDedupExpiries = new ConcurrentHashMap<>();
    private final Map<String, Instant> cooldownExpiries = new ConcurrentHashMap<>();

    public DedupDecision checkAndAcquire(String vehicleId,
                                         long ruleId,
                                         Instant eventTime,
                                         int cooldownSeconds) {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(eventTime, "eventTime must not be null");
        if (cooldownSeconds < 0) {
            throw new IllegalArgumentException("cooldownSeconds must be >= 0");
        }

        String minuteBucketKey = minuteBucketKey(vehicleId, ruleId, eventTime);
        if (isKeyActive(minuteBucketDedupExpiries, minuteBucketKey, eventTime)) {
            return DedupDecision.BLOCKED_BY_MINUTE_BUCKET;
        }

        String cooldownKey = cooldownKey(vehicleId, ruleId);
        if (isKeyActive(cooldownExpiries, cooldownKey, eventTime)) {
            return DedupDecision.BLOCKED_BY_COOLDOWN;
        }

        minuteBucketDedupExpiries.put(minuteBucketKey, eventTime.plus(MINUTE_BUCKET_TTL));
        if (cooldownSeconds > 0) {
            cooldownExpiries.put(cooldownKey, eventTime.plusSeconds(cooldownSeconds));
        } else {
            cooldownExpiries.remove(cooldownKey);
        }
        return DedupDecision.ALLOWED;
    }

    private boolean isKeyActive(Map<String, Instant> expiries, String key, Instant now) {
        Instant expiresAt = expiries.get(key);
        if (expiresAt == null) {
            return false;
        }
        if (!now.isBefore(expiresAt)) {
            expiries.remove(key);
            return false;
        }
        return true;
    }

    private String minuteBucketKey(String vehicleId, long ruleId, Instant eventTime) {
        long bucketEpochMinute = eventTime.atOffset(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MINUTES)
                .toEpochSecond() / 60;
        return vehicleId + ":" + ruleId + ":" + bucketEpochMinute;
    }

    private String cooldownKey(String vehicleId, long ruleId) {
        return "cooldown:alert:" + vehicleId + ":" + ruleId;
    }

    public enum DedupDecision {
        ALLOWED,
        BLOCKED_BY_MINUTE_BUCKET,
        BLOCKED_BY_COOLDOWN
    }
}
