package com.example.demo.device.dto;

import java.time.OffsetDateTime;

public record RotateDeviceTokenResponseData(
        Long deviceId,
        String deviceCode,
        String deviceToken,
        OffsetDateTime tokenRotatedAt
) {
}
