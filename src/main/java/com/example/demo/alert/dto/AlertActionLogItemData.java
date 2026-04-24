package com.example.demo.alert.dto;

import java.time.OffsetDateTime;

public class AlertActionLogItemData {

    private Long id;
    private String actionType;
    private Long actionBy;
    private OffsetDateTime actionTime;
    private String actionRemark;

    public AlertActionLogItemData() {
    }

    public AlertActionLogItemData(Long id, String actionType, Long actionBy, OffsetDateTime actionTime, String actionRemark) {
        this.id = id;
        this.actionType = actionType;
        this.actionBy = actionBy;
        this.actionTime = actionTime;
        this.actionRemark = actionRemark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
