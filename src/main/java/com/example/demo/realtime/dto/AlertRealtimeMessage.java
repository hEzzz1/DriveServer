package com.example.demo.realtime.dto;

import java.time.OffsetDateTime;

public class AlertRealtimeMessage {

    private String type;
    private OffsetDateTime timestamp;
    private AlertPushPayload payload;

    public AlertRealtimeMessage() {
    }

    public AlertRealtimeMessage(String type, OffsetDateTime timestamp, AlertPushPayload payload) {
        this.type = type;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AlertPushPayload getPayload() {
        return payload;
    }

    public void setPayload(AlertPushPayload payload) {
        this.payload = payload;
    }
}
