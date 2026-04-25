package com.example.demo.system.dto;

import java.time.OffsetDateTime;

public record SystemServiceStatusItemData(
        String service,
        String status,
        String description,
        OffsetDateTime lastCheckedAt
) {
}

