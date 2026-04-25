package com.example.demo.rule.dto;

import java.util.List;

public record RuleConfigPageResponseData(
        long total,
        int page,
        int size,
        List<RuleConfigListItemData> items
) {
}

