package com.example.demo.realtime.listener;

import com.example.demo.alert.event.AlertRealtimeEvent;
import com.example.demo.realtime.dto.AlertRealtimeData;
import com.example.demo.realtime.dto.AlertRealtimeMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AlertRealtimeEventListener {

    public static final String ALERT_TOPIC = "/topic/alerts";

    private final SimpMessagingTemplate simpMessagingTemplate;

    public AlertRealtimeEventListener(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertRealtimeEvent(AlertRealtimeEvent event) {
        AlertRealtimeData data = new AlertRealtimeData(
                event.getAlertId(),
                event.getAlertNo(),
                event.getStatus(),
                event.getRiskLevel(),
                event.getRiskScore(),
                event.getFatigueScore(),
                event.getDistractionScore(),
                event.getTriggerTime(),
                event.getFleetId(),
                event.getVehicleId(),
                event.getDriverId(),
                event.getLatestActionBy(),
                event.getLatestActionTime(),
                event.getRemark());
        AlertRealtimeMessage message = new AlertRealtimeMessage(event.getEventType(), event.getTraceId(), data);
        simpMessagingTemplate.convertAndSend(ALERT_TOPIC, message);
    }
}
