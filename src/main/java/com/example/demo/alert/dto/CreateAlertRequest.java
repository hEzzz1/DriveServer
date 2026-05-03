package com.example.demo.alert.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CreateAlertRequest {

    @NotNull
    private Long enterpriseId;

    @NotNull
    private Long fleetId;

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long driverId;

    private Long deviceId;

    private Long sessionId;

    private Long reportedEnterpriseId;

    private Long reportedFleetId;

    private Long reportedVehicleId;

    private Long reportedDriverId;

    private Long resolvedEnterpriseId;

    private Long resolvedFleetId;

    private Long resolvedVehicleId;

    private Long resolvedDriverId;

    @Size(max = 32)
    private String resolutionStatus;

    @Size(max = 64)
    private String configVersion;

    @NotNull
    private Long ruleId;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer riskLevel;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal riskScore;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal fatigueScore;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal distractionScore;

    @NotNull
    private OffsetDateTime triggerTime;

    @Size(max = 255)
    private String remark;

    @Size(max = 32)
    private String edgeRiskLevel;

    @Size(max = 32)
    private String edgeDominantRiskType;

    @Size(max = 255)
    private String edgeTriggerReasons;

    @Min(0)
    private Long edgeWindowStartMs;

    @Min(0)
    private Long edgeWindowEndMs;

    @Min(0)
    private Long edgeCreatedAtMs;

    @Size(max = 32)
    private String evidenceType;

    private String evidenceUrl;

    @Size(max = 64)
    private String evidenceMimeType;

    @Min(0)
    private Long evidenceCapturedAtMs;

    private OffsetDateTime evidenceRetentionUntil;

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Long enterpriseId) {
        this.enterpriseId = enterpriseId;
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getReportedEnterpriseId() {
        return reportedEnterpriseId;
    }

    public void setReportedEnterpriseId(Long reportedEnterpriseId) {
        this.reportedEnterpriseId = reportedEnterpriseId;
    }

    public Long getReportedFleetId() {
        return reportedFleetId;
    }

    public void setReportedFleetId(Long reportedFleetId) {
        this.reportedFleetId = reportedFleetId;
    }

    public Long getReportedVehicleId() {
        return reportedVehicleId;
    }

    public void setReportedVehicleId(Long reportedVehicleId) {
        this.reportedVehicleId = reportedVehicleId;
    }

    public Long getReportedDriverId() {
        return reportedDriverId;
    }

    public void setReportedDriverId(Long reportedDriverId) {
        this.reportedDriverId = reportedDriverId;
    }

    public Long getResolvedEnterpriseId() {
        return resolvedEnterpriseId;
    }

    public void setResolvedEnterpriseId(Long resolvedEnterpriseId) {
        this.resolvedEnterpriseId = resolvedEnterpriseId;
    }

    public Long getResolvedFleetId() {
        return resolvedFleetId;
    }

    public void setResolvedFleetId(Long resolvedFleetId) {
        this.resolvedFleetId = resolvedFleetId;
    }

    public Long getResolvedVehicleId() {
        return resolvedVehicleId;
    }

    public void setResolvedVehicleId(Long resolvedVehicleId) {
        this.resolvedVehicleId = resolvedVehicleId;
    }

    public Long getResolvedDriverId() {
        return resolvedDriverId;
    }

    public void setResolvedDriverId(Long resolvedDriverId) {
        this.resolvedDriverId = resolvedDriverId;
    }

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
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
