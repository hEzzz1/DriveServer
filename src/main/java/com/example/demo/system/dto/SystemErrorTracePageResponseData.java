package com.example.demo.system.dto;

import java.util.List;

public record SystemErrorTracePageResponseData(
        long total,
        int page,
        int size,
        List<SystemErrorTraceItemData> items
) {
}
