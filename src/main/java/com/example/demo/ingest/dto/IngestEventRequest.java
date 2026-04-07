package com.example.demo.ingest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class IngestEventRequest {

    @NotBlank
    @Size(max = 64)
    private String eventId;

    @NotBlank
    @Size(max = 64)
    private String fleetId;

    @NotBlank
    @Size(max = 64)
    private String vehicleId;

    @NotBlank
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
}
