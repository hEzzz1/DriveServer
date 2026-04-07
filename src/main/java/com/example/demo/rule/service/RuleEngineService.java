package com.example.demo.rule.service;

import com.example.demo.rule.model.RuleDefinition;
import com.example.demo.rule.model.RuleEvaluationResult;
import com.example.demo.rule.model.RuleEvent;
import com.example.demo.rule.model.RuleSuppressionReason;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class RuleEngineService {

    private final RiskScoreCalculator riskScoreCalculator;
    private final DurationJudge durationJudge;
    private final AlertCooldownDeduplicator alertCooldownDeduplicator;

    public RuleEngineService(RiskScoreCalculator riskScoreCalculator,
                             DurationJudge durationJudge,
                             AlertCooldownDeduplicator alertCooldownDeduplicator) {
        this.riskScoreCalculator = riskScoreCalculator;
        this.durationJudge = durationJudge;
        this.alertCooldownDeduplicator = alertCooldownDeduplicator;
    }

    public RuleEvaluationResult evaluate(RuleEvent event, List<RuleDefinition> rules) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(rules, "rules must not be null");

        BigDecimal riskScore = riskScoreCalculator.calculate(event.getFatigueScore(), event.getDistractionScore());
        List<RuleDefinition> orderedRules = rules.stream()
                .filter(RuleDefinition::isEnabled)
                .sorted(Comparator.comparingInt((RuleDefinition rule) -> rule.getRiskLevel().getCode()).reversed())
                .toList();

        RuleDefinition highestMatchedByScoreRule = null;
        RuleSuppressionReason highestMatchedSuppressionReason = RuleSuppressionReason.SCORE_BELOW_THRESHOLD;

        for (RuleDefinition rule : orderedRules) {
            boolean scoreMatched = riskScore.compareTo(rule.getRiskThreshold()) >= 0;
            boolean durationMatched = durationJudge.hasReachedDuration(
                    event.getVehicleId(),
                    rule.getRuleId(),
                    event.getEventTime().toInstant(),
                    scoreMatched,
                    rule.getDurationSeconds()
            );

            if (!scoreMatched) {
                continue;
            }

            if (highestMatchedByScoreRule == null) {
                highestMatchedByScoreRule = rule;
                highestMatchedSuppressionReason = RuleSuppressionReason.DURATION_NOT_MET;
            }

            if (!durationMatched) {
                continue;
            }

            AlertCooldownDeduplicator.DedupDecision dedupDecision = alertCooldownDeduplicator.checkAndAcquire(
                    event.getVehicleId(),
                    rule.getRuleId(),
                    event.getEventTime().toInstant(),
                    rule.getCooldownSeconds()
            );

            if (dedupDecision == AlertCooldownDeduplicator.DedupDecision.ALLOWED) {
                return RuleEvaluationResult.triggered(riskScore, rule);
            }

            if (highestMatchedByScoreRule == rule) {
                highestMatchedSuppressionReason = dedupDecision == AlertCooldownDeduplicator.DedupDecision.BLOCKED_BY_MINUTE_BUCKET
                        ? RuleSuppressionReason.DEDUP_IN_MINUTE_BUCKET
                        : RuleSuppressionReason.IN_COOLDOWN;
            }

            // 当前规则已满足分值和持续时长，但在去重/冷却期内，直接抑制，不再降级触发更低等级规则。
            return RuleEvaluationResult.suppressed(riskScore, rule, highestMatchedSuppressionReason);
        }

        if (highestMatchedByScoreRule != null) {
            return RuleEvaluationResult.suppressed(riskScore, highestMatchedByScoreRule, highestMatchedSuppressionReason);
        }
        return RuleEvaluationResult.normal(riskScore);
    }
}
