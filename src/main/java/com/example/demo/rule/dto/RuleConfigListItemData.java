package com.example.demo.rule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RuleConfigListItemData(
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
        OffsetDateTime updatedAt,
        Long alertCount,
        Long falsePositiveCount,
        BigDecimal falsePositiveRate
) {
}
