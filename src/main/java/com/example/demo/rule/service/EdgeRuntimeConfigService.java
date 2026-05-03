package com.example.demo.rule.service;

import com.example.demo.rule.dto.EdgeConfigResponseData;
import com.example.demo.rule.model.RiskLevel;
import com.example.demo.rule.model.RuleDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EdgeRuntimeConfigService {

    private static final BigDecimal DEFAULT_LOW_RISK_THRESHOLD = new BigDecimal("0.48");
    private static final BigDecimal DEFAULT_MEDIUM_RISK_THRESHOLD = new BigDecimal("0.65");
    private static final BigDecimal DEFAULT_HIGH_RISK_THRESHOLD = new BigDecimal("0.80");
    private static final long DEFAULT_TRIGGER_DURATION_MS = 2_000L;
    private static final long DEFAULT_DEBOUNCE_WINDOW_MS = 8_000L;

    private final RuleDefinitionProvider ruleDefinitionProvider;
    private final EdgeConfigVersionResolver edgeConfigVersionResolver;

    public EdgeRuntimeConfigService(RuleDefinitionProvider ruleDefinitionProvider,
                                    EdgeConfigVersionResolver edgeConfigVersionResolver) {
        this.ruleDefinitionProvider = ruleDefinitionProvider;
        this.edgeConfigVersionResolver = edgeConfigVersionResolver;
    }

    @Transactional(readOnly = true)
    public EdgeConfigResponseData currentConfig() {
        List<RuleDefinition> rules = ruleDefinitionProvider.loadEnabledRuleDefinitions();
        Map<RiskLevel, RuleDefinition> rulesByLevel = rules.stream()
                .collect(Collectors.toMap(RuleDefinition::getRiskLevel, rule -> rule, (left, right) -> left));
        long triggerDurationMs = resolveTriggerDurationMs(rules);

        EdgeConfigResponseData.RiskConfigData riskConfig = new EdgeConfigResponseData.RiskConfigData(
                new BigDecimal("0.24"),
                triggerDurationMs,
                1,
                30_000L,
                Math.max(1_500L, triggerDurationMs),
                new BigDecimal("0.50"),
                new BigDecimal("0.45"),
                Math.max(1_000L, triggerDurationMs / 2),
                new BigDecimal("0.42"),
                Math.max(1_000L, triggerDurationMs / 2),
                2,
                2,
                new BigDecimal("0.03"),
                new BigDecimal("0.08"),
                thresholdFor(rulesByLevel, RiskLevel.LOW, DEFAULT_LOW_RISK_THRESHOLD),
                thresholdFor(rulesByLevel, RiskLevel.MID, DEFAULT_MEDIUM_RISK_THRESHOLD),
                thresholdFor(rulesByLevel, RiskLevel.HIGH, DEFAULT_HIGH_RISK_THRESHOLD));

        EdgeConfigResponseData.TemporalConfigData temporalConfig = new EdgeConfigResponseData.TemporalConfigData(
                4_000L,
                3,
                2,
                2,
                new BigDecimal("0.65"),
                80L,
                600L,
                700L,
                new BigDecimal("0.35"),
                riskConfig.distractionHeadDownThreshold(),
                riskConfig.distractionGazeOffsetThreshold(),
                riskConfig.distractionHeadPoseStabilityThreshold(),
                riskConfig.distractionHeadDownDurationMs(),
                riskConfig.distractionGazeOffsetDurationMs());

        EdgeConfigResponseData.UploadPolicyData uploadPolicy = new EdgeConfigResponseData.UploadPolicyData(
                resolveDebounceWindowMs(rules),
                8,
                24,
                List.of(5_000L, 15_000L, 30_000L, 60_000L));

        EdgeConfigResponseData.EvidencePolicyData evidencePolicy = new EdgeConfigResponseData.EvidencePolicyData(
                true,
                "KEY_FRAME",
                "image/jpeg",
                68,
                96 * 1024,
                30);

        return new EdgeConfigResponseData(
                edgeConfigVersionResolver.resolveCurrentVersion(),
                OffsetDateTime.now(ZoneOffset.UTC),
                riskConfig,
                temporalConfig,
                uploadPolicy,
                evidencePolicy);
    }

    private BigDecimal thresholdFor(Map<RiskLevel, RuleDefinition> rulesByLevel,
                                    RiskLevel level,
                                    BigDecimal fallback) {
        RuleDefinition rule = rulesByLevel.get(level);
        return rule == null ? fallback : rule.getRiskThreshold();
    }

    private long resolveTriggerDurationMs(List<RuleDefinition> rules) {
        return rules.stream()
                .map(RuleDefinition::getDurationSeconds)
                .filter(value -> value > 0)
                .min(Comparator.naturalOrder())
                .map(value -> Math.max(1_000L, value * 1_000L))
                .orElse(DEFAULT_TRIGGER_DURATION_MS);
    }

    private long resolveDebounceWindowMs(List<RuleDefinition> rules) {
        return rules.stream()
                .map(RuleDefinition::getCooldownSeconds)
                .filter(value -> value > 0)
                .min(Comparator.naturalOrder())
                .map(value -> Math.max(5_000L, Math.min(30_000L, value * 1_000L)))
                .orElse(DEFAULT_DEBOUNCE_WINDOW_MS);
    }
}
