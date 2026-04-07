package com.example.demo.alert.dto;

import java.time.OffsetDateTime;

public class AlertActionLogItemData {

    private String actionType;
    private Long actionBy;
    private OffsetDateTime actionTime;
    private String actionRemark;

    public AlertActionLogItemData() {
    }

    public AlertActionLogItemData(String actionType, Long actionBy, OffsetDateTime actionTime, String actionRemark) {
        this.actionType = actionType;
        this.actionBy = actionBy;
        this.actionTime = actionTime;
        this.actionRemark = actionRemark;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getActionBy() {
        return actionBy;
    }

    public void setActionBy(Long actionBy) {
        this.actionBy = actionBy;
    }

    public OffsetDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(OffsetDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public String getActionRemark() {
        return actionRemark;
    }

    public void setActionRemark(String actionRemark) {
        this.actionRemark = actionRemark;
    }
}
