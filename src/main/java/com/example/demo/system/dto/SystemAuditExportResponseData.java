package com.example.demo.system.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SystemAuditExportResponseData(
        OffsetDateTime exportedAt,
        long total,
        List<SystemAuditListItemData> items
) {
}

