package com.example.demo.alert.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AlertListItemData {

    private Long id;
    private String alertNo;
    private Long fleetId;
    private Long vehicleId;
    private Long driverId;
    private Integer riskLevel;
    private BigDecimal fatigueScore;
    private BigDecimal distractionScore;
    private Integer status;
    private OffsetDateTime triggerTime;

    public AlertListItemData(Long id,
                             String alertNo,
                             Long fleetId,
                             Long vehicleId,
                             Long driverId,
                             Integer riskLevel,
                             BigDecimal fatigueScore,
                             BigDecimal distractionScore,
                             Integer status,
                             OffsetDateTime triggerTime) {
        this.id = id;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.riskLevel = riskLevel;
        this.fatigueScore = fatigueScore;
        this.distractionScore = distractionScore;
        this.status = status;
        this.triggerTime = triggerTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getFatigueScore() {
        return fatigueScore;
    }

    public void setFatigueScore(BigDecimal fatigueScore) {
        this.fatigueScore = fatigueScore;
    }

    public BigDecimal getDistractionScore() {
        return distractionScore;
    }

    public void setDistractionScore(BigDecimal distractionScore) {
        this.distractionScore = distractionScore;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public OffsetDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(OffsetDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }
}
