package com.example.demo.session.dto;

import java.time.OffsetDateTime;

public record SessionAdminDetailResponseData(
        Long id,
        String sessionNo,
        Long enterpriseId,
        String enterpriseName,
        Long fleetId,
        String fleetName,
        Long vehicleId,
        String vehiclePlateNumber,
        Long driverId,
        String driverCode,
        String driverName,
        Long deviceId,
        String deviceCode,
        OffsetDateTime signInTime,
        OffsetDateTime signOutTime,
        Byte status,
        String closedReason,
        String remark,
        OffsetDateTime lastHeartbeatAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
