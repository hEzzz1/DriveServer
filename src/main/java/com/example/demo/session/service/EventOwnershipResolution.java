package com.example.demo.session.service;

import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.ResolutionStatus;

public record EventOwnershipResolution(
        Long deviceId,
        Long sessionId,
        Long reportedEnterpriseId,
        Long reportedFleetId,
        Long reportedVehicleId,
        Long reportedDriverId,
        Long resolvedEnterpriseId,
        Long resolvedFleetId,
        Long resolvedVehicleId,
        Long resolvedDriverId,
        ResolutionStatus resolutionStatus,
        DrivingSession activeSession
) {
}
