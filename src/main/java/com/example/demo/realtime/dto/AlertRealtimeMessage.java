package com.example.demo.realtime.dto;

public class AlertRealtimeMessage {

    private String eventType;
    private String traceId;
    private AlertRealtimeData data;

    public AlertRealtimeMessage() {
    }

    public AlertRealtimeMessage(String eventType, String traceId, AlertRealtimeData data) {
        this.eventType = eventType;
        this.traceId = traceId;
        this.data = data;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public AlertRealtimeData getData() {
        return data;
    }

    public void setData(AlertRealtimeData data) {
        this.data = data;
    }
}
