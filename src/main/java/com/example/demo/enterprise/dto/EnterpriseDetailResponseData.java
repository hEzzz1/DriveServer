package com.example.demo.enterprise.dto;

import java.time.OffsetDateTime;

public record EnterpriseDetailResponseData(
        Long id,
        String code,
        String name,
        Byte status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
