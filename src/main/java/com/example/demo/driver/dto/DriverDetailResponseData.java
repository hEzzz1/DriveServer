package com.example.demo.driver.dto;

import java.time.OffsetDateTime;

public record DriverDetailResponseData(
        Long id,
        Long enterpriseId,
        Long fleetId,
        String name,
        String phone,
        String licenseNo,
        Byte status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
