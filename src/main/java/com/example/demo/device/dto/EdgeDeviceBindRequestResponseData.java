package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record EdgeDeviceBindRequestResponseData(
        Long id,
        Long deviceId,
        String deviceCode,
        Long requestedEnterpriseId,
        String status,
        String applyRemark,
        String reviewRemark,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        Long reviewedBy,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
