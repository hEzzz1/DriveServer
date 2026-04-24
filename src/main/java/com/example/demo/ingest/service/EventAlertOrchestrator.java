package com.example.demo.ingest.service;

import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.service.AlertService;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.rule.model.RuleDefinition;
import com.example.demo.rule.model.RuleEvaluationResult;
import com.example.demo.rule.model.RuleEvent;
import com.example.demo.rule.service.RuleEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class EventAlertOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EventAlertOrchestrator.class);
    private static final List<RuleDefinition> DEFAULT_RULES = RuleDefinition.defaultRiskRules();
    private static final AuthenticatedUser SYSTEM_OPERATOR =
            new AuthenticatedUser(1L, "system-auto-alert", List.of("ADMIN"));

    private final RuleEngineService ruleEngineService;
    private final AlertService alertService;

    public EventAlertOrchestrator(RuleEngineService ruleEngineService, AlertService alertService) {
        this.ruleEngineService = ruleEngineService;
        this.alertService = alertService;
    }

    public void process(IngestEventRequest request) {
        RuleEvent ruleEvent = new RuleEvent(
                request.getVehicleId(),
                request.getEventTime(),
                request.getFatigueScore(),
                request.getDistractionScore()
        );
        RuleEvaluationResult result = ruleEngineService.evaluate(ruleEvent, DEFAULT_RULES);
        if (!result.isTriggered()) {
            log.info("EVENT_ALERT_SKIPPED eventId={} vehicleId={} reason={} riskScore={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    result.getSuppressionReason(),
                    result.getRiskScore());
            return;
        }

        try {
            CreateAlertRequest createRequest = toCreateAlertRequest(request, result);
            alertService.createAlert(createRequest, SYSTEM_OPERATOR);
            log.info("EVENT_ALERT_CREATED eventId={} vehicleId={} ruleId={} riskLevel={} riskScore={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    createRequest.getRuleId(),
                    createRequest.getRiskLevel(),
                    createRequest.getRiskScore());
        } catch (IllegalArgumentException ex) {
            log.warn("EVENT_ALERT_SKIPPED eventId={} vehicleId={} reason={}",
                    request.getEventId(),
                    request.getVehicleId(),
                    ex.getMessage());
        }
    }

    private CreateAlertRequest toCreateAlertRequest(IngestEventRequest request, RuleEvaluationResult result) {
        RuleDefinition matchedRule = result.getMatchedRule();
        if (matchedRule == null) {
            throw new IllegalArgumentException("matched rule is missing for triggered event");
        }

        CreateAlertRequest createRequest = new CreateAlertRequest();
        createRequest.setFleetId(parseBusinessId(request.getFleetId(), "fleetId"));
        createRequest.setVehicleId(parseBusinessId(request.getVehicleId(), "vehicleId"));
        createRequest.setDriverId(parseBusinessId(request.getDriverId(), "driverId"));
        createRequest.setRuleId(matchedRule.getRuleId());
        createRequest.setRiskLevel(result.getRiskLevel().getCode());
        createRequest.setRiskScore(scale(result.getRiskScore()));
        createRequest.setFatigueScore(scale(request.getFatigueScore()));
        createRequest.setDistractionScore(scale(request.getDistractionScore()));
        createRequest.setTriggerTime(request.getEventTime());
        createRequest.setRemark("Auto created from event " + request.getEventId());
        return createRequest;
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
}
