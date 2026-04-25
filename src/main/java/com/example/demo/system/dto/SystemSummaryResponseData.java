package com.example.demo.system.dto;

import java.time.OffsetDateTime;

public record SystemSummaryResponseData(
        OffsetDateTime generatedAt,
        SystemHealthResponseData health,
        SystemServicesResponseData services,
        SystemVersionResponseData version,
        SystemMonitoringResponseData monitoring
) {
}

