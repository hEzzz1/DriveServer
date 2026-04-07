package com.example.demo.rule.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public class RuleEvent {

    private final String vehicleId;
    private final OffsetDateTime eventTime;
    private final BigDecimal fatigueScore;
    private final BigDecimal distractionScore;

    public RuleEvent(String vehicleId,
                     OffsetDateTime eventTime,
                     BigDecimal fatigueScore,
                     BigDecimal distractionScore) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId must not be null").trim();
        this.eventTime = Objects.requireNonNull(eventTime, "eventTime must not be null");
        this.fatigueScore = Objects.requireNonNull(fatigueScore, "fatigueScore must not be null");
        this.distractionScore = Objects.requireNonNull(distractionScore, "distractionScore must not be null");
        if (this.vehicleId.isEmpty()) {
            throw new IllegalArgumentException("vehicleId must not be blank");
        }
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public BigDecimal getFatigueScore() {
        return fatigueScore;
    }

    public BigDecimal getDistractionScore() {
        return distractionScore;
    }
}
