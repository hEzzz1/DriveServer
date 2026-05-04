package com.example.demo.system.dto;

import java.time.OffsetDateTime;

public record SystemErrorTraceItemData(
        Long id,
        String traceId,
        OffsetDateTime occurredAt,
        String method,
        String requestPath,
        String queryString,
        Integer httpStatus,
        Integer code,
        String message,
        String exceptionClass,
        String summary,
        Long operatorId,
        String ip,
        String userAgent
) {
}
