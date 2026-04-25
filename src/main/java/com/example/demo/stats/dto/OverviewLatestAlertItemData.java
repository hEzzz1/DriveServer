package com.example.demo.stats.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class OverviewLatestAlertItemData {

    private Long id;
    private String alertNo;
    private Long fleetId;
    private Long vehicleId;
    private Long driverId;
    private Integer riskLevel;
    private Integer status;
    private BigDecimal riskScore;
    private OffsetDateTime triggerTime;

    public OverviewLatestAlertItemData() {
    }

    public OverviewLatestAlertItemData(Long id,
                                       String alertNo,
                                       Long fleetId,
                                       Long vehicleId,
                                       Long driverId,
                                       Integer riskLevel,
                                       Integer status,
                                       BigDecimal riskScore,
                                       OffsetDateTime triggerTime) {
        this.id = id;
        this.alertNo = alertNo;
        this.fleetId = fleetId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.riskScore = riskScore;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public OffsetDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(OffsetDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }
}
