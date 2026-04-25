package com.example.demo.system.dto;

import java.time.OffsetDateTime;

public record SystemAuditListItemData(
        Long id,
        Long operatorId,
        String operatorName,
        String module,
        String action,
        String targetId,
        String actionType,
        Long actionBy,
        OffsetDateTime actionTime,
        String actionTargetType,
        String actionTargetId,
        String actionResult,
        String actionRemark,
        String traceId,
        String ip,
        String userAgent
) {
}

