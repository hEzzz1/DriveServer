package com.example.demo.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserListItemData(
        Long id,
        String username,
        String nickname,
        boolean enabled,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
