package com.example.demo.stats.dto;

import com.example.demo.stats.model.RankingDimension;
import com.example.demo.stats.model.RankingSortBy;

import java.time.OffsetDateTime;
import java.util.List;

public class RankingResponseData {

    private RankingDimension dimension;
    private RankingSortBy sortBy;
    private int limit;
    private Long fleetId;
    private Integer riskLevel;
    private Integer status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int totalDimensionCount;
    private List<RankingItemData> items;

    public RankingResponseData() {
    }

    public RankingResponseData(RankingDimension dimension,
                               RankingSortBy sortBy,
                               int limit,
                               Long fleetId,
                               Integer riskLevel,
                               Integer status,
                               OffsetDateTime startTime,
                               OffsetDateTime endTime,
                               int totalDimensionCount,
                               List<RankingItemData> items) {
        this.dimension = dimension;
        this.sortBy = sortBy;
        this.limit = limit;
        this.fleetId = fleetId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalDimensionCount = totalDimensionCount;
        this.items = items;
    }

    public RankingDimension getDimension() {
        return dimension;
    }

    public void setDimension(RankingDimension dimension) {
        this.dimension = dimension;
    }

    public RankingSortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(RankingSortBy sortBy) {
        this.sortBy = sortBy;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
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

    public int getTotalDimensionCount() {
        return totalDimensionCount;
    }

    public void setTotalDimensionCount(int totalDimensionCount) {
        this.totalDimensionCount = totalDimensionCount;
    }

    public List<RankingItemData> getItems() {
        return items;
    }

    public void setItems(List<RankingItemData> items) {
        this.items = items;
    }
}
