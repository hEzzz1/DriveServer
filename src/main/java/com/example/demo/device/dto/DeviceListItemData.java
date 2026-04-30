package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceListItemData(
        Long id,
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        String deviceCode,
        String deviceName,
        String activationCode,
        String status,
        String lifecycleStatus,
        String enterpriseBindStatus,
        String vehicleBindStatus,
        String sessionStage,
        String effectiveStage,
        OffsetDateTime lastActivatedAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime tokenRotatedAt,
        Long currentDriverId,
        String currentDriverCode,
        String currentDriverName,
        Long currentSessionId,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
