package com.example.demo.alert.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AlertListItemData {

    private Long id;
    private String alertNo;
    private Long fleetId;
    private String fleetName;
    private Long vehicleId;
    private String vehiclePlateNumber;
    private Long driverId;
    private String driverName;
    private String driverCode;
    private Long deviceId;
    private String deviceCode;
    private Long ruleId;
    private String ruleName;
    private Integer riskLevel;
    private BigDecimal fatigueScore;
    private BigDecimal distractionScore;
    private Integer status;
    private OffsetDateTime triggerTime;
    private String evidenceType;
    private String evidenceUrl;
    private String evidenceMimeType;
    private Long evidenceCapturedAtMs;
    private OffsetDateTime evidenceRetentionUntil;

    public AlertListItemData(Long id,
                             String alertNo,
                             Long fleetId,
                             String fleetName,
                             Long vehicleId,
                             String vehiclePlateNumber,
                             Long driverId,
                             String driverName,
                             String driverCode,
                             Long deviceId,
                             String deviceCode,
                             Long ruleId,
                             String ruleName,
                             Integer riskLevel,
                             BigDecimal fatigueScore,
                             BigDecimal distractionScore,
                             Integer status,
                             OffsetDateTime triggerTime,
                             String evidenceType,
                             String evidenceUrl,
                             String evidenceMimeType,
                             Long evidenceCapturedAtMs,
                             OffsetDateTime evidenceRetentionUntil) {
        this.id = id;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.fleetName = fleetName;
        this.vehicleId = vehicleId;
        this.vehiclePlateNumber = vehiclePlateNumber;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverCode = driverCode;
        this.deviceId = deviceId;
        this.deviceCode = deviceCode;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.riskLevel = riskLevel;
        this.fatigueScore = fatigueScore;
        this.distractionScore = distractionScore;
        this.status = status;
        this.triggerTime = triggerTime;
        this.evidenceType = evidenceType;
        this.evidenceUrl = evidenceUrl;
        this.evidenceMimeType = evidenceMimeType;
        this.evidenceCapturedAtMs = evidenceCapturedAtMs;
        this.evidenceRetentionUntil = evidenceRetentionUntil;
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

    public String getFleetName() {
        return fleetName;
    }

    public void setFleetName(String fleetName) {
        this.fleetName = fleetName;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehiclePlateNumber() {
        return vehiclePlateNumber;
    }

    public void setVehiclePlateNumber(String vehiclePlateNumber) {
        this.vehiclePlateNumber = vehiclePlateNumber;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getEvidenceMimeType() {
        return evidenceMimeType;
    }

    public void setEvidenceMimeType(String evidenceMimeType) {
        this.evidenceMimeType = evidenceMimeType;
    }

    public Long getEvidenceCapturedAtMs() {
        return evidenceCapturedAtMs;
    }

    public void setEvidenceCapturedAtMs(Long evidenceCapturedAtMs) {
        this.evidenceCapturedAtMs = evidenceCapturedAtMs;
    }

    public OffsetDateTime getEvidenceRetentionUntil() {
        return evidenceRetentionUntil;
    }

    public void setEvidenceRetentionUntil(OffsetDateTime evidenceRetentionUntil) {
        this.evidenceRetentionUntil = evidenceRetentionUntil;
    }
}
