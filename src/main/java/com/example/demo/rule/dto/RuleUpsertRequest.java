package com.example.demo.rule.dto;

import com.example.demo.rule.model.RuleConfigStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RuleUpsertRequest(
        @NotBlank @Size(max = 64) String ruleCode,
        @NotBlank @Size(max = 128) String ruleName,
        @Min(1) @Max(3) int riskLevel,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal riskThreshold,
        @Min(1) int durationSeconds,
        @Min(0) int cooldownSeconds,
        Boolean enabled,
        RuleConfigStatus status,
        @Size(max = 255) String changeRemark
) {
}
