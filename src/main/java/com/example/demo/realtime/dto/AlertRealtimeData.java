package com.example.demo.realtime.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AlertRealtimeData {

    private Long alertId;
    private String alertNo;
    private Integer status;
    private Integer riskLevel;
    private BigDecimal riskScore;
    private BigDecimal fatigueScore;
    private BigDecimal distractionScore;
    private OffsetDateTime triggerTime;
    private Long fleetId;
    private Long vehicleId;
    private Long driverId;
    private Long latestActionBy;
    private OffsetDateTime latestActionTime;
    private String remark;

    public AlertRealtimeData() {
    }

    public AlertRealtimeData(Long alertId,
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Integer riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
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

    public OffsetDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(OffsetDateTime triggerTime) {
        this.triggerTime = triggerTime;
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

    public Long getLatestActionBy() {
        return latestActionBy;
    }

    public void setLatestActionBy(Long latestActionBy) {
        this.latestActionBy = latestActionBy;
    }

    public OffsetDateTime getLatestActionTime() {
        return latestActionTime;
    }

    public void setLatestActionTime(OffsetDateTime latestActionTime) {
        this.latestActionTime = latestActionTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
