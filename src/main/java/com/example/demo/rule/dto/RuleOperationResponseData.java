package com.example.demo.rule.dto;

import java.time.OffsetDateTime;

public record RuleOperationResponseData(
        Long id,
        String ruleCode,
        Integer version,
        Boolean enabled,
        String status,
        String actionType,
        OffsetDateTime actionTime,
        String summary
) {
}

