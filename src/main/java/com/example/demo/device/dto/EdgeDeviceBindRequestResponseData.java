package com.example.demo.device.dto;

import java.util.List;
import java.time.OffsetDateTime;

public record EdgeDeviceBindRequestResponseData(
        Long id,
        Long deviceId,
        String deviceCode,
        String deviceName,
        String activationCode,
        Long enterpriseId,
        Long requestedEnterpriseId,
        String enterpriseName,
        String status,
        String applyRemark,
        String approveRemark,
        String rejectReason,
        String reviewRemark,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        Long reviewedBy,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastSeenAt,
        String effectiveStage,
        DeviceBindingViewData.BindRequestDeviceData device,
        List<DeviceBindingViewData.BindRequestHistoryItemData> history
) {
}
