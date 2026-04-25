package com.example.demo.system.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SystemMonitoringResponseData(
        OffsetDateTime snapshotAt,
        Long openAlertCount,
        Long alertCount24h,
        Long auditCount24h,
        Long enabledRuleCount,
        BigDecimal averageRiskScore24h
) {
}

