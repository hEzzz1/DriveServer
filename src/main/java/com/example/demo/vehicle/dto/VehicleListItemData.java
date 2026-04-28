package com.example.demo.vehicle.dto;

import java.time.OffsetDateTime;

public record VehicleListItemData(
        Long id,
        Long enterpriseId,
        Long fleetId,
        String plateNumber,
        String vin,
        Byte status,
        Long boundDeviceId,
        String boundDeviceCode,
        String boundDeviceName,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
