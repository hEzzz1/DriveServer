package com.example.demo.stats.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class StatsOverviewResponseData {

    private Long fleetId;
    private OffsetDateTime windowStartTime;
    private OffsetDateTime windowEndTime;
    private long alertCountLast5Minutes;
    private long highRiskCountLast5Minutes;
    private long handledCountLast5Minutes;
    private List<OverviewLatestAlertItemData> latestAlerts;
    private List<OverviewRiskDistributionItemData> riskDistribution;

    public StatsOverviewResponseData() {
    }

    public StatsOverviewResponseData(Long fleetId,
                                     OffsetDateTime windowStartTime,
                                     OffsetDateTime windowEndTime,
                                     long alertCountLast5Minutes,
                                     long highRiskCountLast5Minutes,
                                     long handledCountLast5Minutes,
                                     List<OverviewLatestAlertItemData> latestAlerts,
                                     List<OverviewRiskDistributionItemData> riskDistribution) {
        this.fleetId = fleetId;
        this.windowStartTime = windowStartTime;
        this.windowEndTime = windowEndTime;
        this.alertCountLast5Minutes = alertCountLast5Minutes;
        this.highRiskCountLast5Minutes = highRiskCountLast5Minutes;
        this.handledCountLast5Minutes = handledCountLast5Minutes;
        this.latestAlerts = latestAlerts;
        this.riskDistribution = riskDistribution;
    }

    public Long getFleetId() {
        return fleetId;
    }

    public void setFleetId(Long fleetId) {
        this.fleetId = fleetId;
    }

    public OffsetDateTime getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(OffsetDateTime windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public OffsetDateTime getWindowEndTime() {
        return windowEndTime;
    }

    public void setWindowEndTime(OffsetDateTime windowEndTime) {
        this.windowEndTime = windowEndTime;
    }

    public long getAlertCountLast5Minutes() {
        return alertCountLast5Minutes;
    }

    public void setAlertCountLast5Minutes(long alertCountLast5Minutes) {
        this.alertCountLast5Minutes = alertCountLast5Minutes;
    }

    public long getHighRiskCountLast5Minutes() {
        return highRiskCountLast5Minutes;
    }

    public void setHighRiskCountLast5Minutes(long highRiskCountLast5Minutes) {
        this.highRiskCountLast5Minutes = highRiskCountLast5Minutes;
    }

    public long getHandledCountLast5Minutes() {
        return handledCountLast5Minutes;
    }

    public void setHandledCountLast5Minutes(long handledCountLast5Minutes) {
        this.handledCountLast5Minutes = handledCountLast5Minutes;
    }

    public List<OverviewLatestAlertItemData> getLatestAlerts() {
        return latestAlerts;
    }

    public void setLatestAlerts(List<OverviewLatestAlertItemData> latestAlerts) {
        this.latestAlerts = latestAlerts;
    }

    public List<OverviewRiskDistributionItemData> getRiskDistribution() {
        return riskDistribution;
    }

    public void setRiskDistribution(List<OverviewRiskDistributionItemData> riskDistribution) {
        this.riskDistribution = riskDistribution;
    }
}
