package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceContextResponseData(
        Long deviceId,
        String deviceCode,
        String deviceName,
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        Long currentSessionId,
        String currentSessionNo,
        OffsetDateTime currentSessionSignInTime
) {
}
