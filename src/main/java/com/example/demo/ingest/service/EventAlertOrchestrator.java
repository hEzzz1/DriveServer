package com.example.demo.ingest.service;

import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.service.AlertService;
import com.example.demo.device.entity.Device;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.rule.model.RuleDefinition;
import com.example.demo.rule.model.RuleEvaluationResult;
import com.example.demo.rule.model.RiskLevel;
import com.example.demo.rule.model.RuleEvent;
import com.example.demo.rule.service.RuleDefinitionProvider;
import com.example.demo.rule.service.RuleEngineService;
import com.example.demo.session.service.EventOwnershipResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EventAlertOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EventAlertOrchestrator.class);
    private static final String SYSTEM_OPERATOR_USERNAME = "system-auto-alert";

    private final RuleEngineService ruleEngineService;
    private final AlertService alertService;
    private final RuleDefinitionProvider ruleDefinitionProvider;
    private final UserAccountRepository userAccountRepository;

    public EventAlertOrchestrator(RuleEngineService ruleEngineService,
                                  AlertService alertService,
                                  RuleDefinitionProvider ruleDefinitionProvider,
                                  UserAccountRepository userAccountRepository) {
        this.ruleEngineService = ruleEngineService;
        this.alertService = alertService;
        this.ruleDefinitionProvider = ruleDefinitionProvider;
        this.userAccountRepository = userAccountRepository;
    }

    public void process(IngestEventRequest request, Device device, EventOwnershipResolution resolution) {
        try {
            List<RuleDefinition> activeRules = ruleDefinitionProvider.loadEnabledRuleDefinitions();
            if (activeRules.isEmpty()) {
                log.info("INGEST_WARNING_SKIPPED eventId={} vehicleId={} reason=no_enabled_rules",
                        request.getEventId(),
                        request.getVehicleId());
                return;
            }
            CreateAlertRequest createRequest = hasCompleteEdgeWarning(request)
                    ? toEdgeAlertRequest(request, activeRules, device, resolution)
                    : toFallbackAlertRequest(request, activeRules, device, resolution);
            alertService.createAlert(createRequest, systemOperator());
            log.info("INGEST_WARNING_CREATED eventId={} vehicleId={} ruleId={} riskLevel={} riskScore={} edgeRiskLevel={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    createRequest.getRuleId(),
                    createRequest.getRiskLevel(),
                    createRequest.getRiskScore(),
                    createRequest.getEdgeRiskLevel());
        } catch (IllegalArgumentException ex) {
            log.warn("INGEST_WARNING_SKIPPED eventId={} vehicleId={} reason={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    ex.getMessage());
        }
    }

    private CreateAlertRequest toEdgeAlertRequest(IngestEventRequest request, List<RuleDefinition> activeRules, Device device, EventOwnershipResolution resolution) {
        int riskLevelCode = parseEdgeRiskLevel(request.getRiskLevel());
        RuleDefinition matchedRule = findRuleByRiskLevelCode(activeRules, riskLevelCode);
        return buildCreateAlertRequest(request, matchedRule.getRuleId(), riskLevelCode, maxRiskScore(request), device, resolution);
    }

    private CreateAlertRequest toFallbackAlertRequest(IngestEventRequest request, List<RuleDefinition> activeRules, Device device, EventOwnershipResolution resolution) {
        RuleEvaluationResult result = ruleEngineService.evaluate(toRuleEvent(request), activeRules);
        RuleDefinition matchedRule = result.isTriggered()
                ? Objects.requireNonNull(result.getMatchedRule(), "matched rule is missing for triggered event")
                : resolveRuleByScore(activeRules, result.getRiskScore());
        return buildCreateAlertRequest(
                request,
                matchedRule.getRuleId(),
                matchedRule.getRiskLevel().getCode(),
                result.getRiskScore(),
                device,
                resolution);
    }

    private CreateAlertRequest buildCreateAlertRequest(IngestEventRequest request,
                                                       Long ruleId,
                                                       Integer riskLevel,
                                                       BigDecimal riskScore,
                                                       Device device,
                                                       EventOwnershipResolution resolution) {
        CreateAlertRequest createRequest = new CreateAlertRequest();
        createRequest.setEnterpriseId(resolution.resolvedEnterpriseId());
        createRequest.setFleetId(resolution.resolvedFleetId());
        createRequest.setVehicleId(resolution.resolvedVehicleId() == null ? device.getVehicleId() : resolution.resolvedVehicleId());
        createRequest.setDriverId(resolution.resolvedDriverId() == null ? 0L : resolution.resolvedDriverId());
        createRequest.setDeviceId(resolution.deviceId());
        createRequest.setSessionId(resolution.sessionId());
        createRequest.setReportedEnterpriseId(resolution.reportedEnterpriseId());
        createRequest.setReportedFleetId(resolution.reportedFleetId());
        createRequest.setReportedVehicleId(resolution.reportedVehicleId());
        createRequest.setReportedDriverId(resolution.reportedDriverId());
        createRequest.setResolvedEnterpriseId(resolution.resolvedEnterpriseId());
        createRequest.setResolvedFleetId(resolution.resolvedFleetId());
        createRequest.setResolvedVehicleId(resolution.resolvedVehicleId());
        createRequest.setResolvedDriverId(resolution.resolvedDriverId());
        createRequest.setResolutionStatus(resolution.resolutionStatus().name());
        createRequest.setConfigVersion(normalizeOptionalText(request.getConfigVersion()));
        createRequest.setRuleId(ruleId);
        createRequest.setRiskLevel(riskLevel);
        createRequest.setRiskScore(scale(riskScore));
        createRequest.setFatigueScore(scale(request.getFatigueScore()));
        createRequest.setDistractionScore(scale(request.getDistractionScore()));
        createRequest.setEdgeRiskLevel(normalizeOptionalText(request.getRiskLevel()));
        createRequest.setEdgeDominantRiskType(normalizeOptionalText(request.getDominantRiskType()));
        createRequest.setEdgeTriggerReasons(joinTriggerReasons(request.getTriggerReasons()));
        createRequest.setEdgeWindowStartMs(request.getWindowStartMs());
        createRequest.setEdgeWindowEndMs(request.getWindowEndMs());
        createRequest.setEdgeCreatedAtMs(request.getCreatedAtMs());
        createRequest.setEvidenceType(normalizeOptionalText(request.getEvidenceType()));
        createRequest.setEvidenceUrl(normalizeOptionalText(request.getEvidenceUrl()));
        createRequest.setEvidenceMimeType(normalizeOptionalText(request.getEvidenceMimeType()));
        createRequest.setEvidenceCapturedAtMs(request.getEvidenceCapturedAtMs());
        createRequest.setEvidenceRetentionUntil(null);
        createRequest.setTriggerTime(request.getEventTime());
        createRequest.setRemark("Warning accepted from event " + request.getEventId());
        return createRequest;
    }

    private RuleEvent toRuleEvent(IngestEventRequest request) {
        return new RuleEvent(
                request.getVehicleId(),
                request.getEventTime(),
                request.getFatigueScore(),
                request.getDistractionScore()
        );
    }

    private boolean hasCompleteEdgeWarning(IngestEventRequest request) {
        return StringUtils.hasText(request.getRiskLevel())
                && StringUtils.hasText(request.getDominantRiskType())
                && request.getTriggerReasons() != null
                && !request.getTriggerReasons().isEmpty();
    }

    private int parseEdgeRiskLevel(String rawRiskLevel) {
        String normalized = normalizeOptionalText(rawRiskLevel);
        if (normalized == null) {
            throw new IllegalArgumentException("edge riskLevel is missing");
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "HIGH", "H", "3" -> RiskLevel.HIGH.getCode();
            case "MID", "MEDIUM", "M", "2" -> RiskLevel.MID.getCode();
            case "LOW", "L", "1" -> RiskLevel.LOW.getCode();
            default -> throw new IllegalArgumentException("unsupported edge riskLevel: " + rawRiskLevel);
        };
    }

    private RuleDefinition resolveRuleByScore(List<RuleDefinition> activeRules, BigDecimal riskScore) {
        BigDecimal normalizedScore = scale(riskScore);
        return activeRules.stream()
                .sorted(RuleDefinition.BY_RISK_LEVEL_DESC)
                .filter(rule -> normalizedScore.compareTo(rule.getRiskThreshold()) >= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no enabled rule matched current risk score"));
    }

    private RuleDefinition findRuleByRiskLevelCode(List<RuleDefinition> activeRules, int riskLevelCode) {
        return activeRules.stream()
                .filter(rule -> rule.getRiskLevel().getCode() == riskLevelCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported riskLevel code: " + riskLevelCode));
    }

    private BigDecimal maxRiskScore(IngestEventRequest request) {
        return request.getFatigueScore().max(request.getDistractionScore());
    }

    private String joinTriggerReasons(List<String> triggerReasons) {
        if (triggerReasons == null || triggerReasons.isEmpty()) {
            return null;
        }
        return triggerReasons.stream()
                .map(this::normalizeOptionalText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private Long parseBusinessId(String rawValue, String fieldName) {
        if (rawValue == null) {
            throw new IllegalArgumentException(fieldName + " is missing");
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is blank");
        }

        if (trimmed.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(trimmed);
        }

        int end = trimmed.length();
        while (end > 0 && !Character.isDigit(trimmed.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && Character.isDigit(trimmed.charAt(start - 1))) {
            start--;
        }
        if (start == end) {
            throw new IllegalArgumentException(fieldName + " must contain a numeric suffix: " + rawValue);
        }

        return Long.parseLong(trimmed.substring(start, end));
    }

    private Long parseOptionalBusinessId(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return 0L;
        }
        return parseBusinessId(rawValue, "optionalBusinessId");
    }

    private AuthenticatedUser systemOperator() {
        UserAccount account = userAccountRepository
                .findByUsernameAndSubjectType(SYSTEM_OPERATOR_USERNAME, SubjectType.SYSTEM.name())
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, "系统内部账号未初始化"));
        return new AuthenticatedUser(account.getId(), account.getUsername(), SubjectType.SYSTEM, List.of());
    }
}
