package com.example.demo.rule.model;

public enum RuleSuppressionReason {
    NONE,
    SCORE_BELOW_THRESHOLD,
    DURATION_NOT_MET,
    DEDUP_IN_MINUTE_BUCKET,
    IN_COOLDOWN
}
