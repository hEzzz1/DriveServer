package com.example.demo.alert.dto;

import java.util.List;

public class AlertActionLogsResponseData {

    private Long alertId;
    private List<AlertActionLogItemData> items;

    public AlertActionLogsResponseData() {
    }

    public AlertActionLogsResponseData(Long alertId, List<AlertActionLogItemData> items) {
        this.alertId = alertId;
        this.items = items;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public List<AlertActionLogItemData> getItems() {
        return items;
    }

    public void setItems(List<AlertActionLogItemData> items) {
        this.items = items;
    }
}
