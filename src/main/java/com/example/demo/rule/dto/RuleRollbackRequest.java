package com.example.demo.rule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RuleRollbackRequest(
        @Min(1) int versionNo,
        @Size(max = 255) String changeRemark
) {
}

