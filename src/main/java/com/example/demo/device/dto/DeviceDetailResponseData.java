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
        String status,
        String lifecycleStatus,
        String enterpriseBindStatus,
        String vehicleBindStatus,
        String sessionStage,
        String effectiveStage,
        DeviceBindingViewData.NamedResourceData enterprise,
        DeviceBindingViewData.NamedResourceData fleet,
        DeviceBindingViewData.VehicleData vehicle,
        OffsetDateTime lastActivatedAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime tokenRotatedAt,
        Long currentDriverId,
        String currentDriverCode,
        String currentDriverName,
        Long currentSessionId,
        DeviceBindingViewData.DriverData currentDriver,
        DeviceBindingViewData.SessionData activeSession,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
