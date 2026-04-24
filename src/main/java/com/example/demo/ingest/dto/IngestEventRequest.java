package com.example.demo.ingest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class IngestEventRequest {

    @Size(max = 64)
    private String eventId;

    @Size(max = 64)
    private String fleetId;

    @Size(max = 64)
    private String vehicleId;

    @Size(max = 64)
    private String driverId;

    @NotNull
    private OffsetDateTime eventTime;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal fatigueScore;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal distractionScore;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal perclos;

    @DecimalMin("0.0")
    private BigDecimal blinkRate;

    @Min(0)
    private Integer yawnCount;

    @Size(max = 32)
    private String headPose;

    @Size(max = 32)
    private String algorithmVer;

    @Size(max = 32)
    private String riskLevel;

    @Size(max = 32)
    private String dominantRiskType;

    private List<String> triggerReasons;

    private Long windowStartMs;

    private Long windowEndMs;

    private Long createdAtMs;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getFleetId() {
        return fleetId;
    }

    public void setFleetId(String fleetId) {
        this.fleetId = fleetId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(OffsetDateTime eventTime) {
        this.eventTime = eventTime;
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

    public BigDecimal getPerclos() {
        return perclos;
    }

    public void setPerclos(BigDecimal perclos) {
        this.perclos = perclos;
    }

    public BigDecimal getBlinkRate() {
        return blinkRate;
    }

    public void setBlinkRate(BigDecimal blinkRate) {
        this.blinkRate = blinkRate;
    }

    public Integer getYawnCount() {
        return yawnCount;
    }

    public void setYawnCount(Integer yawnCount) {
        this.yawnCount = yawnCount;
    }

    public String getHeadPose() {
        return headPose;
    }

    public void setHeadPose(String headPose) {
        this.headPose = headPose;
    }

    public String getAlgorithmVer() {
        return algorithmVer;
    }

    public void setAlgorithmVer(String algorithmVer) {
        this.algorithmVer = algorithmVer;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getDominantRiskType() {
        return dominantRiskType;
    }

    public void setDominantRiskType(String dominantRiskType) {
        this.dominantRiskType = dominantRiskType;
    }

    public List<String> getTriggerReasons() {
        return triggerReasons;
    }

    public void setTriggerReasons(List<String> triggerReasons) {
        this.triggerReasons = triggerReasons;
    }

    public Long getWindowStartMs() {
        return windowStartMs;
    }

    public void setWindowStartMs(Long windowStartMs) {
        this.windowStartMs = windowStartMs;
    }

    public Long getWindowEndMs() {
        return windowEndMs;
    }

    public void setWindowEndMs(Long windowEndMs) {
        this.windowEndMs = windowEndMs;
    }

    public Long getCreatedAtMs() {
        return createdAtMs;
    }

    public void setCreatedAtMs(Long createdAtMs) {
        this.createdAtMs = createdAtMs;
    }
}
