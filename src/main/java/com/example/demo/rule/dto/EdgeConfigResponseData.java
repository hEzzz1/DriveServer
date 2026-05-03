package com.example.demo.rule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record EdgeConfigResponseData(
        String configVersion,
        OffsetDateTime updatedAt,
        RiskConfigData risk,
        TemporalConfigData temporal,
        UploadPolicyData uploadPolicy,
        EvidencePolicyData evidencePolicy
) {
    public record RiskConfigData(
            BigDecimal fatiguePerclosThreshold,
            Long fatiguePerclosDurationMs,
            Integer fatigueYawnCountThreshold,
            Long fatigueYawnWindowMaxMs,
            Long distractionHeadPoseDurationMs,
            BigDecimal distractionHeadPoseStabilityThreshold,
            BigDecimal distractionHeadDownThreshold,
            Long distractionHeadDownDurationMs,
            BigDecimal distractionGazeOffsetThreshold,
            Long distractionGazeOffsetDurationMs,
            Integer triggerConfirmCount,
            Integer clearConfirmCount,
            BigDecimal triggerHysteresisDelta,
            BigDecimal clearHysteresisDelta,
            BigDecimal lowRiskThreshold,
            BigDecimal mediumRiskThreshold,
            BigDecimal highRiskThreshold
    ) {
    }

    public record TemporalConfigData(
            Long windowSizeMs,
            Integer smoothingWindowCount,
            Integer stableWindowHitCount,
            Integer clearWindowCount,
            BigDecimal featureEmaAlpha,
            Long blinkMinDurationMs,
            Long blinkMaxDurationMs,
            Long yawnMinDurationMs,
            BigDecimal minSignalConfidence,
            BigDecimal headDownThreshold,
            BigDecimal gazeOffsetThreshold,
            BigDecimal headPoseStabilityThreshold,
            Long headDownMinDurationMs,
            Long gazeOffsetMinDurationMs
    ) {
    }

    public record UploadPolicyData(
            Long debounceWindowMs,
            Integer batchSize,
            Integer maxBatchSize,
            List<Long> retryBackoffMs
    ) {
    }

    public record EvidencePolicyData(
            Boolean enabled,
            String type,
            String mimeType,
            Integer jpegQuality,
            Integer maxBytes,
            Integer retentionDays
    ) {
    }
}
