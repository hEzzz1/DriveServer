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

    @Column(name = "fleet_id", nullable = false)
    private Long fleetId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

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
