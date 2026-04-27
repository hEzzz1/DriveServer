package com.example.demo.enterprise.dto;

import java.time.OffsetDateTime;

public record EnterpriseDetailResponseData(
        Long id,
        String code,
        String name,
        boolean enabled,
        String contactName,
        String contactPhone,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
