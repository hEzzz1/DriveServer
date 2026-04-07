package com.example.demo.rule.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DurationJudge {

    private final Map<String, Instant> hitStartTimes = new ConcurrentHashMap<>();

    public boolean hasReachedDuration(String vehicleId,
                                      long ruleId,
                                      Instant eventTime,
                                      boolean thresholdHit,
                                      int durationSeconds) {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(eventTime, "eventTime must not be null");
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }

        String key = buildKey(vehicleId, ruleId);
        if (!thresholdHit) {
            hitStartTimes.remove(key);
            return false;
        }

        Instant startTime = hitStartTimes.compute(key, (k, existing) -> {
            if (existing == null || eventTime.isBefore(existing)) {
                return eventTime;
            }
            return existing;
        });

        long continuousSeconds = Duration.between(startTime, eventTime).getSeconds();
        return continuousSeconds >= durationSeconds;
    }

    private String buildKey(String vehicleId, long ruleId) {
        return vehicleId + ":" + ruleId;
    }
}
