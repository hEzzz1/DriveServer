package com.example.demo.rule;

import com.example.demo.rule.model.RiskLevel;
import com.example.demo.rule.model.RuleDefinition;
import com.example.demo.rule.model.RuleEvaluationResult;
import com.example.demo.rule.model.RuleEvent;
import com.example.demo.rule.model.RuleSuppressionReason;
import com.example.demo.rule.service.AlertCooldownDeduplicator;
import com.example.demo.rule.service.DurationJudge;
import com.example.demo.rule.service.RiskScoreCalculator;
import com.example.demo.rule.service.RuleEngineService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceTest {

    private final RuleEngineService service = new RuleEngineService(
            new RiskScoreCalculator(),
            new DurationJudge(),
            new AlertCooldownDeduplicator()
    );

    @Test
    void shouldTriggerHighRiskWhenDurationReached() {
        List<RuleDefinition> rules = RuleDefinition.defaultRiskRules();
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-04-07T10:00:00Z");

        RuleEvaluationResult at0 = service.evaluate(highRiskEvent("veh_001", baseTime), rules);
        RuleEvaluationResult at2 = service.evaluate(highRiskEvent("veh_001", baseTime.plusSeconds(2)), rules);
        RuleEvaluationResult at3 = service.evaluate(highRiskEvent("veh_001", baseTime.plusSeconds(3)), rules);

        assertThat(at0.isTriggered()).isFalse();
        assertThat(at2.isTriggered()).isFalse();
        assertThat(at3.isTriggered()).isTrue();
        assertThat(at3.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shouldTriggerMidRiskWhenHighNotMatched() {
        List<RuleDefinition> rules = RuleDefinition.defaultRiskRules();
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-04-07T10:10:00Z");

        RuleEvaluationResult at0 = service.evaluate(midRiskEvent("veh_002", baseTime), rules);
        RuleEvaluationResult at4 = service.evaluate(midRiskEvent("veh_002", baseTime.plusSeconds(4)), rules);
        RuleEvaluationResult at5 = service.evaluate(midRiskEvent("veh_002", baseTime.plusSeconds(5)), rules);

        assertThat(at0.isTriggered()).isFalse();
        assertThat(at4.isTriggered()).isFalse();
        assertThat(at5.isTriggered()).isTrue();
        assertThat(at5.getRiskLevel()).isEqualTo(RiskLevel.MID);
    }

    @Test
    void shouldSuppressByCooldownAndRecoverAfterWindow() {
        List<RuleDefinition> rules = RuleDefinition.defaultRiskRules();
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-04-07T11:00:00Z");

        service.evaluate(highRiskEvent("veh_003", baseTime), rules);
        service.evaluate(highRiskEvent("veh_003", baseTime.plusSeconds(1)), rules);
        RuleEvaluationResult triggered = service.evaluate(highRiskEvent("veh_003", baseTime.plusSeconds(3)), rules);
        RuleEvaluationResult blocked = service.evaluate(highRiskEvent("veh_003", baseTime.plusSeconds(62)), rules);
        RuleEvaluationResult recovered = service.evaluate(highRiskEvent("veh_003", baseTime.plusSeconds(64)), rules);

        assertThat(triggered.isTriggered()).isTrue();
        assertThat(blocked.isTriggered()).isFalse();
        assertThat(blocked.getSuppressionReason()).isEqualTo(RuleSuppressionReason.IN_COOLDOWN);
        assertThat(recovered.isTriggered()).isTrue();
    }

    @Test
    void shouldReturnNormalWhenNoRuleMatched() {
        List<RuleDefinition> rules = RuleDefinition.defaultRiskRules();
        RuleEvaluationResult result = service.evaluate(
                lowScoreEvent("veh_004", OffsetDateTime.parse("2026-04-07T12:00:00Z")),
                rules
        );

        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.NORMAL);
        assertThat(result.getSuppressionReason()).isEqualTo(RuleSuppressionReason.SCORE_BELOW_THRESHOLD);
    }

    private RuleEvent highRiskEvent(String vehicleId, OffsetDateTime eventTime) {
        return new RuleEvent(vehicleId, eventTime, new BigDecimal("0.92"), new BigDecimal("0.90"));
    }

    private RuleEvent midRiskEvent(String vehicleId, OffsetDateTime eventTime) {
        return new RuleEvent(vehicleId, eventTime, new BigDecimal("0.70"), new BigDecimal("0.70"));
    }

    private RuleEvent lowScoreEvent(String vehicleId, OffsetDateTime eventTime) {
        return new RuleEvent(vehicleId, eventTime, new BigDecimal("0.30"), new BigDecimal("0.20"));
    }
}
