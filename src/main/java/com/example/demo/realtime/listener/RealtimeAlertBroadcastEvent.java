package com.example.demo.realtime.listener;

import com.example.demo.realtime.dto.RealtimeAlertMessageData;

public record RealtimeAlertBroadcastEvent(
        Long enterpriseId,
        Long fleetId,
        RealtimeAlertMessageData payload
) {
}
