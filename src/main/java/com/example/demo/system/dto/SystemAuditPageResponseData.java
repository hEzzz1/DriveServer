package com.example.demo.system.dto;

import java.util.List;

public record SystemAuditPageResponseData(
        long total,
        int page,
        int size,
        List<SystemAuditListItemData> items
) {
}

