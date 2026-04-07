package com.example.demo.rule.model;

import java.math.BigDecimal;
import java.util.Objects;

public class RuleEvaluationResult {

    private final boolean triggered;
    private final BigDecimal riskScore;
    private final RiskLevel riskLevel;
    private final RuleDefinition matchedRule;
    private final RuleSuppressionReason suppressionReason;

    private RuleEvaluationResult(boolean triggered,
                                 BigDecimal riskScore,
                                 RiskLevel riskLevel,
                                 RuleDefinition matchedRule,
                                 RuleSuppressionReason suppressionReason) {
        this.triggered = triggered;
        this.riskScore = Objects.requireNonNull(riskScore, "riskScore must not be null");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.matchedRule = matchedRule;
        this.suppressionReason = Objects.requireNonNull(suppressionReason, "suppressionReason must not be null");
    }

    public static RuleEvaluationResult triggered(BigDecimal riskScore, RuleDefinition matchedRule) {
        Objects.requireNonNull(matchedRule, "matchedRule must not be null");
        return new RuleEvaluationResult(true, riskScore, matchedRule.getRiskLevel(), matchedRule, RuleSuppressionReason.NONE);
    }

    public static RuleEvaluationResult suppressed(BigDecimal riskScore,
                                                  RuleDefinition matchedRule,
                                                  RuleSuppressionReason suppressionReason) {
        RiskLevel level = matchedRule == null ? RiskLevel.NORMAL : matchedRule.getRiskLevel();
        return new RuleEvaluationResult(false, riskScore, level, matchedRule, suppressionReason);
    }

    public static RuleEvaluationResult normal(BigDecimal riskScore) {
        return new RuleEvaluationResult(false, riskScore, RiskLevel.NORMAL, null, RuleSuppressionReason.SCORE_BELOW_THRESHOLD);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public RuleDefinition getMatchedRule() {
        return matchedRule;
    }

    public RuleSuppressionReason getSuppressionReason() {
        return suppressionReason;
    }
}
