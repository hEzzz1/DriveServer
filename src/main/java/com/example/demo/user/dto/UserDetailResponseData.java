package com.example.demo.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailResponseData(
        Long id,
        String username,
        String nickname,
        Long enterpriseId,
        String enterpriseName,
        boolean enabled,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
