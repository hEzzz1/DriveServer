package com.example.demo.alert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_event")
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_no", nullable = false, length = 64, unique = true)
    private String alertNo;

    @Column(name = "enterprise_id")
    private Long enterpriseId;

    @Column(name = "fleet_id", nullable = false)
    private Long fleetId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "reported_enterprise_id")
    private Long reportedEnterpriseId;

    @Column(name = "reported_fleet_id")
    private Long reportedFleetId;

    @Column(name = "reported_vehicle_id")
    private Long reportedVehicleId;

    @Column(name = "reported_driver_id")
    private Long reportedDriverId;

    @Column(name = "resolved_enterprise_id")
    private Long resolvedEnterpriseId;

    @Column(name = "resolved_fleet_id")
    private Long resolvedFleetId;

    @Column(name = "resolved_vehicle_id")
    private Long resolvedVehicleId;

    @Column(name = "resolved_driver_id")
    private Long resolvedDriverId;

    @Column(name = "resolution_status", length = 32)
    private String resolutionStatus;

    @Column(name = "config_version", length = 64)
    private String configVersion;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "risk_level", nullable = false)
    private Byte riskLevel;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "fatigue_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal fatigueScore;

    @Column(name = "distraction_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal distractionScore;

    @Column(name = "edge_risk_level", length = 32)
    private String edgeRiskLevel;

    @Column(name = "edge_dominant_risk_type", length = 32)
    private String edgeDominantRiskType;

    @Column(name = "edge_trigger_reasons", length = 255)
    private String edgeTriggerReasons;

    @Column(name = "edge_window_start_ms")
    private Long edgeWindowStartMs;

    @Column(name = "edge_window_end_ms")
    private Long edgeWindowEndMs;

    @Column(name = "edge_created_at_ms")
    private Long edgeCreatedAtMs;

    @Column(name = "evidence_type", length = 32)
    private String evidenceType;

    @Column(name = "evidence_url", columnDefinition = "LONGTEXT")
    private String evidenceUrl;

    @Column(name = "evidence_mime_type", length = 64)
    private String evidenceMimeType;

    @Column(name = "evidence_captured_at_ms")
    private Long evidenceCapturedAtMs;

    @Column(name = "evidence_retention_until")
    private LocalDateTime evidenceRetentionUntil;

    @Column(name = "trigger_time", nullable = false)
    private LocalDateTime triggerTime;

    @Column(nullable = false)
    private Byte status;

    @Column(name = "latest_action_by")
    private Long latestActionBy;

    @Column(name = "latest_action_time")
    private LocalDateTime latestActionTime;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Byte getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Byte riskLevel) {
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

    public LocalDateTime getEvidenceRetentionUntil() {
        return evidenceRetentionUntil;
    }

    public void setEvidenceRetentionUntil(LocalDateTime evidenceRetentionUntil) {
        this.evidenceRetentionUntil = evidenceRetentionUntil;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Long getLatestActionBy() {
        return latestActionBy;
    }

    public void setLatestActionBy(Long latestActionBy) {
        this.latestActionBy = latestActionBy;
    }

    public LocalDateTime getLatestActionTime() {
        return latestActionTime;
    }

    public void setLatestActionTime(LocalDateTime latestActionTime) {
        this.latestActionTime = latestActionTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
