package com.example.demo.alert.event;

import com.example.demo.alert.entity.AlertEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class AlertRealtimeEvent {

    private final String eventType;
    private final String traceId;
    private final Long alertId;
    private final String alertNo;
    private final Integer status;
    private final Integer riskLevel;
    private final BigDecimal riskScore;
    private final BigDecimal fatigueScore;
    private final BigDecimal distractionScore;
    private final OffsetDateTime triggerTime;
    private final Long fleetId;
    private final Long vehicleId;
    private final Long driverId;
    private final Long latestActionBy;
    private final OffsetDateTime latestActionTime;
    private final String remark;

    private AlertRealtimeEvent(String eventType,
                               String traceId,
                               Long alertId,
                               String alertNo,
                               Integer status,
                               Integer riskLevel,
                               BigDecimal riskScore,
                               BigDecimal fatigueScore,
                               BigDecimal distractionScore,
                               OffsetDateTime triggerTime,
                               Long fleetId,
                               Long vehicleId,
                               Long driverId,
                               Long latestActionBy,
                               OffsetDateTime latestActionTime,
                               String remark) {
        this.eventType = eventType;
        this.traceId = traceId;
        this.alertId = alertId;
        this.alertNo = alertNo;
        this.status = status;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.fatigueScore = fatigueScore;
        this.distractionScore = distractionScore;
        this.triggerTime = triggerTime;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.latestActionBy = latestActionBy;
        this.latestActionTime = latestActionTime;
        this.remark = remark;
    }

    public static AlertRealtimeEvent created(AlertEvent alert, String traceId) {
        return from("ALERT_CREATED", traceId, alert);
    }

    public static AlertRealtimeEvent updated(AlertEvent alert, String traceId) {
        return from("ALERT_UPDATED", traceId, alert);
    }

    private static AlertRealtimeEvent from(String eventType, String traceId, AlertEvent alert) {
        return new AlertRealtimeEvent(
                eventType,
                traceId,
                alert.getId(),
                alert.getAlertNo(),
                alert.getStatus() == null ? null : alert.getStatus().intValue(),
                alert.getRiskLevel() == null ? null : alert.getRiskLevel().intValue(),
                alert.getRiskScore(),
                alert.getFatigueScore(),
                alert.getDistractionScore(),
                toOffsetDateTime(alert.getTriggerTime()),
                alert.getFleetId(),
                alert.getVehicleId(),
                alert.getDriverId(),
                alert.getLatestActionBy(),
                toOffsetDateTime(alert.getLatestActionTime()),
                alert.getRemark());
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    public String getEventType() {
        return eventType;
    }

    public String getTraceId() {
        return traceId;
    }

    public Long getAlertId() {
        return alertId;
    }

    public String getAlertNo() {
        return alertNo;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public BigDecimal getFatigueScore() {
        return fatigueScore;
    }

    public BigDecimal getDistractionScore() {
        return distractionScore;
    }

    public OffsetDateTime getTriggerTime() {
        return triggerTime;
    }

    public Long getFleetId() {
        return fleetId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public Long getLatestActionBy() {
        return latestActionBy;
    }

    public OffsetDateTime getLatestActionTime() {
        return latestActionTime;
    }

    public String getRemark() {
        return remark;
    }
}
