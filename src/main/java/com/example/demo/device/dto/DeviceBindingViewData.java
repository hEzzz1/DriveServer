package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public final class DeviceBindingViewData {

    private DeviceBindingViewData() {
    }

    public record NamedResourceData(
            Long id,
            String name
    ) {
    }

    public record VehicleData(
            Long id,
            String plateNumber
    ) {
    }

    public record DriverData(
            Long id,
            String driverCode,
            String name
    ) {
    }

    public record SessionData(
            Long id,
            String sessionNo,
            OffsetDateTime signInTime,
            Byte status
    ) {
    }

    public record ContextDeviceData(
            Long id,
            String deviceCode,
            String deviceName,
            String lifecycleStatus,
            OffsetDateTime lastActivatedAt,
            OffsetDateTime lastSeenAt
    ) {
    }

    public record ClaimedDeviceData(
            Long id,
            String deviceCode,
            String deviceName,
            String deviceToken,
            String lifecycleStatus
    ) {
    }

    public record BindRequestDeviceData(
            Long id,
            String deviceCode,
            String deviceName,
            Long enterpriseId,
            String enterpriseName,
            Long fleetId,
            String fleetName,
            Long vehicleId,
            String vehiclePlateNumber,
            String lifecycleStatus,
            OffsetDateTime lastActivatedAt,
            OffsetDateTime lastSeenAt
    ) {
    }

    public record BindRequestHistoryItemData(
            Long id,
            String action,
            Long operatorId,
            String operatorName,
            String remark,
            OffsetDateTime createdAt
    ) {
    }
}
