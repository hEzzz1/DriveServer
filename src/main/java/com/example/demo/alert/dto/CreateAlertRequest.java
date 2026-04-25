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
    private Long fleetId;

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long driverId;

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
