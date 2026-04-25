package com.example.demo.rule.dto;

import jakarta.validation.constraints.Size;

public record RulePublishRequest(
        @Size(max = 255) String changeRemark
) {
}

