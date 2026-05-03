package com.example.demo.alert.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AlertDetailResponseData {

    private Long id;
    private String alertNo;
    private Long fleetId;
    private Long vehicleId;
    private Long driverId;
    private Long ruleId;
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

    public AlertDetailResponseData(Long id,
                                   String alertNo,
                                   Long fleetId,
                                   Long vehicleId,
                                   Long driverId,
                                   Long ruleId,
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
                                   Long edgeCreatedAtMs) {
        this.id = id;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.ruleId = ruleId;
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
}
