package com.example.demo.stats.dto;

public class OverviewRiskDistributionItemData {

    private Integer riskLevel;
    private long count;

    public OverviewRiskDistributionItemData() {
    }

    public OverviewRiskDistributionItemData(Integer riskLevel, long count) {
        this.riskLevel = riskLevel;
        this.count = count;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Integer riskLevel) {
        this.riskLevel = riskLevel;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
