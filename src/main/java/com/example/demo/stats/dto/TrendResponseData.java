package com.example.demo.stats.dto;

import com.example.demo.stats.model.TrendGroupBy;

import java.time.OffsetDateTime;
import java.util.List;

public class TrendResponseData {

    private TrendGroupBy groupBy;
    private Long fleetId;
    private Integer riskLevel;
    private Integer status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private List<TrendItemData> items;

    public TrendResponseData() {
    }

    public TrendResponseData(TrendGroupBy groupBy,
                             Long fleetId,
                             Integer riskLevel,
                             Integer status,
                             OffsetDateTime startTime,
                             OffsetDateTime endTime,
                             List<TrendItemData> items) {
        this.groupBy = groupBy;
        this.fleetId = fleetId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.items = items;
    }

    public TrendGroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(TrendGroupBy groupBy) {
        this.groupBy = groupBy;
    }

    public Long getFleetId() {
        return fleetId;
    }

    public void setFleetId(Long fleetId) {
        this.fleetId = fleetId;
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

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public List<TrendItemData> getItems() {
        return items;
    }

    public void setItems(List<TrendItemData> items) {
        this.items = items;
    }
}
