package com.example.demo.realtime.dto;

import com.example.demo.alert.dto.AlertListItemData;

import java.time.OffsetDateTime;

public record RealtimeAlertMessageData(
        String eventType,
        OffsetDateTime eventTime,
        AlertListItemData alert
) {
}
