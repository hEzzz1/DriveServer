package com.example.demo.stats.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TrendItemData {

    private OffsetDateTime bucketTime;
    private long alertCount;
    private long highRiskCount;
    private BigDecimal avgRiskScore;
    private BigDecimal avgFatigueScore;
    private BigDecimal avgDistractionScore;

    public TrendItemData() {
    }

    public TrendItemData(OffsetDateTime bucketTime,
                         long alertCount,
                         long highRiskCount,
                         BigDecimal avgRiskScore,
                         BigDecimal avgFatigueScore,
                         BigDecimal avgDistractionScore) {
        this.bucketTime = bucketTime;
        this.alertCount = alertCount;
        this.highRiskCount = highRiskCount;
        this.avgRiskScore = avgRiskScore;
        this.avgFatigueScore = avgFatigueScore;
        this.avgDistractionScore = avgDistractionScore;
    }

    public OffsetDateTime getBucketTime() {
        return bucketTime;
    }

    public void setBucketTime(OffsetDateTime bucketTime) {
        this.bucketTime = bucketTime;
    }

    public long getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(long alertCount) {
        this.alertCount = alertCount;
    }

    public long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public BigDecimal getAvgRiskScore() {
        return avgRiskScore;
    }

    public void setAvgRiskScore(BigDecimal avgRiskScore) {
        this.avgRiskScore = avgRiskScore;
    }

    public BigDecimal getAvgFatigueScore() {
        return avgFatigueScore;
    }

    public void setAvgFatigueScore(BigDecimal avgFatigueScore) {
        this.avgFatigueScore = avgFatigueScore;
    }

    public BigDecimal getAvgDistractionScore() {
        return avgDistractionScore;
    }

    public void setAvgDistractionScore(BigDecimal avgDistractionScore) {
        this.avgDistractionScore = avgDistractionScore;
    }
}
