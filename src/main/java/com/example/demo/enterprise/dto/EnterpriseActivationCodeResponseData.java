package com.example.demo.enterprise.dto;

import java.time.OffsetDateTime;

public record EnterpriseActivationCodeResponseData(
        Long enterpriseId,
        String enterpriseName,
        String activationCode,
        String activationCodeMasked,
        String status,
        OffsetDateTime rotatedAt,
        OffsetDateTime expiresAt
) {
}
