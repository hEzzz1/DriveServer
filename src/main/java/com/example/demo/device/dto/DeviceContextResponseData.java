package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceContextResponseData(
        Long deviceId,
        String deviceCode,
        String deviceName,
        Long enterpriseId,
        String enterpriseName,
        Long fleetId,
        String fleetName,
        Long vehicleId,
        String vehiclePlateNumber,
        Long currentDriverId,
        String currentDriverCode,
        String currentDriverName,
        Long currentSessionId,
        String currentSessionNo,
        OffsetDateTime currentSessionSignInTime,
        Byte currentSessionStatus,
        String configVersion
) {
}
