package com.example.demo.rule.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RuleDefinition {

    public static final Comparator<RuleDefinition> BY_RISK_LEVEL_DESC =
            Comparator.comparingInt((RuleDefinition rule) -> rule.getRiskLevel().getCode()).reversed();

    private final long ruleId;
    private final String ruleCode;
    private final RiskLevel riskLevel;
    private final BigDecimal riskThreshold;
    private final int durationSeconds;
    private final int cooldownSeconds;
    private final boolean enabled;

    public RuleDefinition(long ruleId,
                          String ruleCode,
                          RiskLevel riskLevel,
                          BigDecimal riskThreshold,
                          int durationSeconds,
                          int cooldownSeconds,
                          boolean enabled) {
        if (ruleId <= 0) {
            throw new IllegalArgumentException("ruleId must be positive");
        }
        this.ruleId = ruleId;
        this.ruleCode = Objects.requireNonNull(ruleCode, "ruleCode must not be null").trim();
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.riskThreshold = Objects.requireNonNull(riskThreshold, "riskThreshold must not be null");
        if (this.ruleCode.isEmpty()) {
            throw new IllegalArgumentException("ruleCode must not be blank");
        }
        if (this.riskThreshold.compareTo(BigDecimal.ZERO) < 0 || this.riskThreshold.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("riskThreshold must be in [0, 1]");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        if (cooldownSeconds < 0) {
            throw new IllegalArgumentException("cooldownSeconds must be >= 0");
        }
        this.durationSeconds = durationSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.enabled = enabled;
    }

    public static List<RuleDefinition> defaultRiskRules() {
        return List.of(
                new RuleDefinition(1L, "RISK_HIGH", RiskLevel.HIGH, new BigDecimal("0.80"), 3, 60, true),
                new RuleDefinition(2L, "RISK_MID", RiskLevel.MID, new BigDecimal("0.65"), 5, 60, true),
                new RuleDefinition(3L, "RISK_LOW", RiskLevel.LOW, new BigDecimal("0.50"), 8, 60, true)
        );
    }

    public long getRuleId() {
        return ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getRiskThreshold() {
        return riskThreshold;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
