package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record DeviceContextResponseData(
        Long deviceId,
        String deviceCode,
        String deviceName,
        String deviceStatus,
        String bindStatus,
        String pendingRequestStatus,
        DeviceBindingViewData.ContextDeviceData device,
        String lifecycleStatus,
        String enterpriseBindStatus,
        String vehicleBindStatus,
        String sessionStage,
        String effectiveStage,
        DeviceBindingViewData.NamedResourceData enterprise,
        DeviceBindingViewData.NamedResourceData fleet,
        DeviceBindingViewData.VehicleData vehicle,
        EdgeDeviceBindRequestResponseData currentBindRequest,
        DeviceBindingViewData.SessionData activeSession,
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
