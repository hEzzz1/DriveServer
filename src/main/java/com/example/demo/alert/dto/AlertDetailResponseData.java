package com.example.demo.alert.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AlertDetailResponseData {

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
    private BigDecimal riskScore;
    private BigDecimal fatigueScore;
    private BigDecimal distractionScore;
    private OffsetDateTime triggerTime;
    private Integer status;
    private Long latestActionBy;
    private OffsetDateTime latestActionTime;
    private String remark;
    private String edgeRiskLevel;
    private String edgeDominantRiskType;
    private String edgeTriggerReasons;
    private Long edgeWindowStartMs;
    private Long edgeWindowEndMs;
    private Long edgeCreatedAtMs;
    private String evidenceType;
    private String evidenceUrl;
    private String evidenceMimeType;
    private Long evidenceCapturedAtMs;
    private OffsetDateTime evidenceRetentionUntil;

    public AlertDetailResponseData(Long id,
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
                                   BigDecimal riskScore,
                                   BigDecimal fatigueScore,
                                   BigDecimal distractionScore,
                                   OffsetDateTime triggerTime,
                                   Integer status,
                                   Long latestActionBy,
                                   OffsetDateTime latestActionTime,
                                   String remark,
                                   String edgeRiskLevel,
                                   String edgeDominantRiskType,
                                   String edgeTriggerReasons,
                                   Long edgeWindowStartMs,
                                   Long edgeWindowEndMs,
                                   Long edgeCreatedAtMs,
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
        this.riskScore = riskScore;
        this.fatigueScore = fatigueScore;
        this.distractionScore = distractionScore;
        this.triggerTime = triggerTime;
        this.status = status;
        this.latestActionBy = latestActionBy;
        this.latestActionTime = latestActionTime;
        this.remark = remark;
        this.edgeRiskLevel = edgeRiskLevel;
        this.edgeDominantRiskType = edgeDominantRiskType;
        this.edgeTriggerReasons = edgeTriggerReasons;
        this.edgeWindowStartMs = edgeWindowStartMs;
        this.edgeWindowEndMs = edgeWindowEndMs;
        this.edgeCreatedAtMs = edgeCreatedAtMs;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getEdgeRiskLevel() {
        return edgeRiskLevel;
    }

    public void setEdgeRiskLevel(String edgeRiskLevel) {
        this.edgeRiskLevel = edgeRiskLevel;
    }

    public String getEdgeDominantRiskType() {
        return edgeDominantRiskType;
    }

    public void setEdgeDominantRiskType(String edgeDominantRiskType) {
        this.edgeDominantRiskType = edgeDominantRiskType;
    }

    public String getEdgeTriggerReasons() {
        return edgeTriggerReasons;
    }

    public void setEdgeTriggerReasons(String edgeTriggerReasons) {
        this.edgeTriggerReasons = edgeTriggerReasons;
    }

    public Long getEdgeWindowStartMs() {
        return edgeWindowStartMs;
    }

    public void setEdgeWindowStartMs(Long edgeWindowStartMs) {
        this.edgeWindowStartMs = edgeWindowStartMs;
    }

    public Long getEdgeWindowEndMs() {
        return edgeWindowEndMs;
    }

    public void setEdgeWindowEndMs(Long edgeWindowEndMs) {
        this.edgeWindowEndMs = edgeWindowEndMs;
    }

    public Long getEdgeCreatedAtMs() {
        return edgeCreatedAtMs;
    }

    public void setEdgeCreatedAtMs(Long edgeCreatedAtMs) {
        this.edgeCreatedAtMs = edgeCreatedAtMs;
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
