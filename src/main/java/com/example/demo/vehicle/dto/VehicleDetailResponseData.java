package com.example.demo.vehicle.dto;

import java.time.OffsetDateTime;

public record VehicleDetailResponseData(
        Long id,
        Long enterpriseId,
        Long fleetId,
        String plateNumber,
        String vin,
        Byte status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
