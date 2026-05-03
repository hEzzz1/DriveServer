package com.example.demo.stats.dto;

import java.math.BigDecimal;

public class RankingItemData {

    private int rank;
    private Long dimensionValue;
    private String dimensionName;
    private long alertCount;
    private long highRiskCount;
    private BigDecimal avgRiskScore;
    private BigDecimal avgFatigueScore;
    private BigDecimal avgDistractionScore;

    public RankingItemData() {
    }

    public RankingItemData(int rank,
                           Long dimensionValue,
                           String dimensionName,
                           long alertCount,
                           long highRiskCount,
                           BigDecimal avgRiskScore,
                           BigDecimal avgFatigueScore,
                           BigDecimal avgDistractionScore) {
        this.rank = rank;
        this.dimensionValue = dimensionValue;
        this.dimensionName = dimensionName;
        this.alertCount = alertCount;
        this.highRiskCount = highRiskCount;
        this.avgRiskScore = avgRiskScore;
        this.avgFatigueScore = avgFatigueScore;
        this.avgDistractionScore = avgDistractionScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getDimensionValue() {
        return dimensionValue;
    }

    public void setDimensionValue(Long dimensionValue) {
        this.dimensionValue = dimensionValue;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public void setDimensionName(String dimensionName) {
        this.dimensionName = dimensionName;
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
