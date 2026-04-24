package com.example.demo.alert.event;

import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.model.AlertActionType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class AlertRealtimeEvent {

    private final String type;
    private final OffsetDateTime timestamp;
    private final Long alertId;
    private final String alertNo;
    private final Long fleetId;
    private final Long vehicleId;
    private final Long driverId;
    private final Integer riskLevel;
    private final Integer status;
    private final String actionType;

    private AlertRealtimeEvent(String type,
                               OffsetDateTime timestamp,
                               Long alertId,
                               String alertNo,
                               Long fleetId,
                               Long vehicleId,
                               Long driverId,
                               Integer riskLevel,
                               Integer status,
                               String actionType) {
        this.type = type;
        this.timestamp = timestamp;
        this.alertId = alertId;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.actionType = actionType;
    }

    public static AlertRealtimeEvent created(AlertEvent alert) {
        return from("ALERT_CREATED", alert, AlertActionType.CREATE);
    }

    public static AlertRealtimeEvent updated(AlertEvent alert, AlertActionType actionType) {
        return from("ALERT_UPDATED", alert, actionType);
    }

    private static AlertRealtimeEvent from(String type, AlertEvent alert, AlertActionType actionType) {
        return new AlertRealtimeEvent(
                type,
                toOffsetDateTime(alert.getLatestActionTime()),
                alert.getId(),
                alert.getAlertNo(),
                alert.getFleetId(),
                alert.getVehicleId(),
                alert.getDriverId(),
                alert.getRiskLevel() == null ? null : alert.getRiskLevel().intValue(),
                alert.getStatus() == null ? null : alert.getStatus().intValue(),
                actionType.name());
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    public String getType() {
        return type;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public Long getAlertId() {
        return alertId;
    }

    public String getAlertNo() {
        return alertNo;
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

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public String getActionType() {
        return actionType;
    }
}
