package com.example.demo.stats.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class OverviewLatestAlertItemData {

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
    private Integer status;
    private BigDecimal riskScore;
    private OffsetDateTime triggerTime;

    public OverviewLatestAlertItemData() {
    }

    public OverviewLatestAlertItemData(Long id,
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
                                       Integer status,
                                       BigDecimal riskScore,
                                       OffsetDateTime triggerTime) {
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
