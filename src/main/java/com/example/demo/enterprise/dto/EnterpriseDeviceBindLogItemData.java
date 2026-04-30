package com.example.demo.enterprise.dto;

import java.time.OffsetDateTime;

public record EnterpriseDeviceBindLogItemData(
        Long id,
        Long deviceId,
        String deviceCode,
        String deviceName,
        Long enterpriseId,
        String enterpriseName,
        String activationCodeMasked,
        String action,
        String operatorType,
        Long operatorId,
        String remark,
        OffsetDateTime createdAt
) {
}
