package com.example.demo.fleet.dto;

import java.time.OffsetDateTime;

public record FleetDetailResponseData(
        Long id,
        Long enterpriseId,
        String name,
        Byte status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
