package com.example.demo.rule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RuleConfigDetailData(
        Long id,
        String ruleCode,
        String ruleName,
        String riskLevel,
        BigDecimal riskThreshold,
        Integer durationSeconds,
        Integer cooldownSeconds,
        Boolean enabled,
        String status,
        Integer version,
        OffsetDateTime publishedAt,
        Long publishedBy,
        OffsetDateTime archivedAt,
        Long archivedBy,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RuleConfigVersionItemData> versions
) {
}

