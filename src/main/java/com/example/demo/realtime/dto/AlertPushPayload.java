package com.example.demo.realtime.dto;

public class AlertPushPayload {

    private Long alertId;
    private String alertNo;
    private Long fleetId;
    private Long vehicleId;
    private Long driverId;
    private Integer riskLevel;
    private Integer status;
    private String actionType;

    public AlertPushPayload() {
    }

    public AlertPushPayload(Long alertId,
                            String alertNo,
                            Long fleetId,
                            Long vehicleId,
                            Long driverId,
                            Integer riskLevel,
                            Integer status,
                            String actionType) {
        this.alertId = alertId;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.actionType = actionType;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getAlertNo() {
        return alertNo;
    }

    public void setAlertNo(String alertNo) {
        this.alertNo = alertNo;
    }

    public Long getFleetId() {
        return fleetId;
    }

    public void setFleetId(Long fleetId) {
        this.fleetId = fleetId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Integer riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}
