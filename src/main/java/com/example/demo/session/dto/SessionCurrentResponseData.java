package com.example.demo.session.dto;

import java.time.OffsetDateTime;

public record SessionCurrentResponseData(
        Long sessionId,
        String sessionNo,
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        Long driverId,
        Long deviceId,
        OffsetDateTime signInTime,
        OffsetDateTime signOutTime,
        Byte status,
        String closedReason,
        String remark
) {
}
