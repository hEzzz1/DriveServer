package com.example.demo.fleet.dto;

import java.time.OffsetDateTime;

public record FleetListItemData(
        Long id,
        Long enterpriseId,
        String name,
        Byte status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
