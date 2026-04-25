package com.example.demo.rule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RuleConfigVersionItemData(
        Long id,
        Integer versionNo,
        String ruleCode,
        String ruleName,
        String riskLevel,
        BigDecimal riskThreshold,
        Integer durationSeconds,
        Integer cooldownSeconds,
        Boolean enabled,
        String status,
        String changeSource,
        String changeSummary,
        Long createdBy,
        OffsetDateTime createdAt
) {
}
