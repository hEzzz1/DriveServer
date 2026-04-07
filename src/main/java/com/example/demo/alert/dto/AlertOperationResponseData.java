package com.example.demo.alert.dto;

import java.time.OffsetDateTime;

public class AlertOperationResponseData {

    private Long id;
    private String alertNo;
    private Integer status;
    private Long latestActionBy;
    private OffsetDateTime latestActionTime;
    private String actionType;

    public AlertOperationResponseData() {
    }

    public AlertOperationResponseData(Long id,
                                      String alertNo,
                                      Integer status,
                                      Long latestActionBy,
                                      OffsetDateTime latestActionTime,
                                      String actionType) {
        this.id = id;
        this.alertNo = alertNo;
        this.status = status;
        this.latestActionBy = latestActionBy;
        this.latestActionTime = latestActionTime;
        this.actionType = actionType;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getLatestActionBy() {
        return latestActionBy;
    }

    public void setLatestActionBy(Long latestActionBy) {
        this.latestActionBy = latestActionBy;
    }

    public OffsetDateTime getLatestActionTime() {
        return latestActionTime;
    }

    public void setLatestActionTime(OffsetDateTime latestActionTime) {
        this.latestActionTime = latestActionTime;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}
