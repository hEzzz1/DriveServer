package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceActivateResponseData(
        Long deviceId,
        String deviceCode,
        String deviceName,
        String deviceToken,
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        OffsetDateTime activatedAt
) {
}
