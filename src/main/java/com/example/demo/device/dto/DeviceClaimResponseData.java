package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceClaimResponseData(
        DeviceBindingViewData.ClaimedDeviceData device,
        DeviceBindingViewData.NamedResourceData enterprise,
        DeviceBindingViewData.NamedResourceData fleet,
        DeviceBindingViewData.VehicleData vehicle,
        String vehicleBindStatus,
        String sessionStage,
        String effectiveStage,
        OffsetDateTime claimedAt
) {
}
