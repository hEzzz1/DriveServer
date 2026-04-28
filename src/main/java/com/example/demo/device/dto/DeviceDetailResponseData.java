package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceDetailResponseData(
        Long id,
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        String deviceCode,
        String deviceName,
        String activationCode,
        Byte status,
        OffsetDateTime lastActivatedAt,
        OffsetDateTime lastOnlineAt,
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
