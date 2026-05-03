package com.example.demo.rule.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.alert.model.AlertStatus;
import com.example.demo.alert.repository.AlertEventRepository;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.rule.dto.RuleConfigDetailData;
import com.example.demo.rule.dto.RuleConfigListItemData;
import com.example.demo.rule.dto.RuleConfigPageResponseData;
import com.example.demo.rule.dto.RuleConfigVersionItemData;
import com.example.demo.rule.dto.RuleOperationResponseData;
import com.example.demo.rule.dto.RulePublishRequest;
import com.example.demo.rule.dto.RuleRollbackRequest;
import com.example.demo.rule.dto.RuleUpsertRequest;
import com.example.demo.rule.entity.RuleConfig;
import com.example.demo.rule.entity.RuleConfigVersion;
import com.example.demo.rule.model.RiskLevel;
import com.example.demo.rule.model.RuleConfigStatus;
import com.example.demo.rule.model.RuleDefinition;
import com.example.demo.rule.repository.RuleConfigRepository;
import com.example.demo.rule.repository.RuleConfigVersionRepository;
import com.example.demo.system.service.SystemAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RuleConfigService implements RuleDefinitionProvider {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final RuleConfigRepository ruleConfigRepository;
    private final RuleConfigVersionRepository ruleConfigVersionRepository;
    private final AlertEventRepository alertEventRepository;
    private final ObjectMapper objectMapper;
    private final SystemAuditService systemAuditService;

    public RuleConfigService(RuleConfigRepository ruleConfigRepository,
                             RuleConfigVersionRepository ruleConfigVersionRepository,
                             AlertEventRepository alertEventRepository,
                             ObjectMapper objectMapper,
                             SystemAuditService systemAuditService) {
        this.ruleConfigRepository = ruleConfigRepository;
        this.ruleConfigVersionRepository = ruleConfigVersionRepository;
        this.alertEventRepository = alertEventRepository;
        this.objectMapper = objectMapper;
        this.systemAuditService = systemAuditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleDefinition> loadEnabledRuleDefinitions() {
        List<RuleDefinition> activeRules = ruleConfigRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> RuleConfigStatus.ENABLED.name().equals(rule.getStatus()))
                .map(this::toRuleDefinition)
                .sorted(RuleDefinition.BY_RISK_LEVEL_DESC)
                .toList();
        validateUniqueActiveRiskLevel(activeRules);
        return activeRules;
    }

    @Transactional(readOnly = true)
    public RuleConfigPageResponseData listRules(Integer page,
                                                Integer size,
                                                String status,
                                                Boolean enabled,
                                                Integer riskLevel,
                                                String keyword) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        String normalizedStatus = normalizeText(status);
        String normalizedKeyword = normalizeText(keyword);

        List<RuleConfigListItemData> items = ruleConfigRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .filter(rule -> normalizedStatus == null || rule.getStatus().equalsIgnoreCase(normalizedStatus))
                .filter(rule -> enabled == null || Objects.equals(rule.getEnabled(), enabled))
                .filter(rule -> riskLevel == null || Objects.equals(rule.getRiskLevel(), riskLevel))
                .filter(rule -> normalizedKeyword == null
                        || containsIgnoreCase(rule.getRuleCode(), normalizedKeyword)
                        || containsIgnoreCase(rule.getRuleName(), normalizedKeyword))
                .map(this::toListItem)
                .toList();

        int fromIndex = Math.min((pageNo - 1) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return new RuleConfigPageResponseData(items.size(), pageNo, pageSize, items.subList(fromIndex, toIndex));
    }

    @Transactional(readOnly = true)
    public RuleConfigDetailData getRule(Long id) {
        RuleConfig rule = getRuleOrThrow(id);
        List<RuleConfigVersionItemData> versions = ruleConfigVersionRepository.findByRuleConfigIdOrderByVersionNoDesc(rule.getId())
                .stream()
                .map(this::toVersionItem)
                .toList();
        return toDetail(rule, versions);
    }

    @Transactional
    public RuleOperationResponseData createRule(RuleUpsertRequest request, AuthenticatedUser operator) {
        if (ruleConfigRepository.findByRuleCode(request.ruleCode()).isPresent()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "ruleCode已存在");
        }
        validateDraftOnlyUpsert(request);
        RuleConfig rule = new RuleConfig();
        applyUpsert(rule, request, operator);
        rule.setVersion(1);
        rule.setStatus(RuleConfigStatus.DRAFT.name());
        rule.setEnabled(false);
        RuleConfig saved = ruleConfigRepository.save(rule);
        systemAuditService.record(operator, "RULE", "CREATE_RULE", "RULE", saved.getId().toString(), "SUCCESS",
                request.changeRemark(), buildAuditDetail(saved, "create", request.changeRemark()));
        return toOperationResponse(saved, "CREATE_RULE", request.changeRemark());
    }

    @Transactional
    public RuleOperationResponseData updateRule(Long id, RuleUpsertRequest request, AuthenticatedUser operator) {
        RuleConfig rule = getRuleOrThrow(id);
        if (!rule.getRuleCode().equals(request.ruleCode()) && ruleConfigRepository.findByRuleCode(request.ruleCode()).isPresent()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "ruleCode已存在");
        }
        if (Boolean.TRUE.equals(rule.getEnabled()) && RuleConfigStatus.ENABLED.name().equals(rule.getStatus())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "已启用规则需先停用后再修改");
        }
        validateDraftOnlyUpsert(request);
        applyUpsert(rule, request, operator);
        rule.setEnabled(false);
        rule.setStatus(resolveEditableStatus(rule.getStatus(), request.status()).name());
        RuleConfig saved = ruleConfigRepository.save(rule);
        systemAuditService.record(operator, "RULE", "UPDATE_RULE", "RULE", saved.getId().toString(), "SUCCESS",
                request.changeRemark(), buildAuditDetail(saved, "update", request.changeRemark()));
        return toOperationResponse(saved, "UPDATE_RULE", request.changeRemark());
    }

    @Transactional
    public RuleOperationResponseData publishRule(Long id, RulePublishRequest request, AuthenticatedUser operator) {
        RuleConfig rule = getRuleOrThrow(id);
        ensureNoOtherActiveRuleWithSameRiskLevel(rule.getId(), rule.getRiskLevel());
        int nextVersion = nextVersion(rule.getId(), rule.getVersion());
        rule.setVersion(nextVersion);
        rule.setStatus(RuleConfigStatus.ENABLED.name());
        rule.setEnabled(true);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        rule.setPublishedAt(now);
        rule.setPublishedBy(operator.getUserId());
        rule.setUpdatedBy(operator.getUserId());
        rule.setUpdatedAt(now);
        RuleConfig saved = ruleConfigRepository.save(rule);
        saveVersionSnapshot(saved, nextVersion, "PUBLISH", request.changeRemark(), operator);
        systemAuditService.record(operator, "RULE", "PUBLISH_RULE", "RULE", saved.getId().toString(), "SUCCESS",
                request.changeRemark(), buildAuditDetail(saved, "publish", request.changeRemark()));
        return toOperationResponse(saved, "PUBLISH_RULE", request.changeRemark());
    }

    @Transactional
    public RuleOperationResponseData toggleRule(Long id, AuthenticatedUser operator) {
        RuleConfig rule = getRuleOrThrow(id);
        if (ruleConfigVersionRepository.findFirstByRuleConfigIdOrderByVersionNoDesc(rule.getId()).isEmpty()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "规则尚未发布，不能切换启停");
        }
        boolean targetEnabled = !Boolean.TRUE.equals(rule.getEnabled());
        if (targetEnabled) {
            ensureNoOtherActiveRuleWithSameRiskLevel(rule.getId(), rule.getRiskLevel());
        }
        rule.setEnabled(targetEnabled);
        rule.setStatus(targetEnabled ? RuleConfigStatus.ENABLED.name() : RuleConfigStatus.DISABLED.name());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        rule.setUpdatedBy(operator.getUserId());
        rule.setUpdatedAt(now);
        RuleConfig saved = ruleConfigRepository.save(rule);
        int nextVersion = nextVersion(saved.getId(), saved.getVersion());
        saved.setVersion(nextVersion);
        ruleConfigRepository.save(saved);
        saveVersionSnapshot(saved, nextVersion, "TOGGLE", targetEnabled ? "启用规则" : "停用规则", operator);
        systemAuditService.record(operator, "RULE", "TOGGLE_RULE", "RULE", saved.getId().toString(), "SUCCESS",
                targetEnabled ? "启用规则" : "停用规则", buildAuditDetail(saved, "toggle", targetEnabled ? "enabled" : "disabled"));
        return toOperationResponse(saved, "TOGGLE_RULE", targetEnabled ? "启用规则" : "停用规则");
    }

    @Transactional(readOnly = true)
    public List<RuleConfigVersionItemData> listVersions(Long id) {
        getRuleOrThrow(id);
        return ruleConfigVersionRepository.findByRuleConfigIdOrderByVersionNoDesc(id).stream()
                .map(this::toVersionItem)
                .toList();
    }

    @Transactional
    public RuleOperationResponseData rollbackRule(Long id, RuleRollbackRequest request, AuthenticatedUser operator) {
        RuleConfig rule = getRuleOrThrow(id);
        RuleConfigVersion version = ruleConfigVersionRepository.findByRuleConfigIdAndVersionNo(id, request.versionNo())
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, "版本不存在"));
        if (Boolean.TRUE.equals(version.getEnabled()) && RuleConfigStatus.ENABLED.name().equals(version.getStatus())) {
            ensureNoOtherActiveRuleWithSameRiskLevel(rule.getId(), version.getRiskLevel());
        }
        int nextVersion = nextVersion(rule.getId(), rule.getVersion());
        applySnapshot(rule, version, operator);
        rule.setVersion(nextVersion);
        RuleConfig saved = ruleConfigRepository.save(rule);
        saveVersionSnapshot(saved, nextVersion, "ROLLBACK", request.changeRemark(), operator);
        systemAuditService.record(operator, "RULE", "ROLLBACK_RULE", "RULE", saved.getId().toString(), "SUCCESS",
                request.changeRemark(), buildAuditDetail(saved, "rollback", request.changeRemark(), "rollbackFrom", request.versionNo()));
        return toOperationResponse(saved, "ROLLBACK_RULE", request.changeRemark());
    }

    private void applyUpsert(RuleConfig rule, RuleUpsertRequest request, AuthenticatedUser operator) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        rule.setRuleCode(request.ruleCode().trim());
        rule.setRuleName(request.ruleName().trim());
        rule.setRiskLevel(request.riskLevel());
        rule.setRiskThreshold(request.riskThreshold().setScale(4));
        rule.setDurationSeconds(request.durationSeconds());
        rule.setCooldownSeconds(request.cooldownSeconds());
        rule.setUpdatedBy(operator.getUserId());
        rule.setUpdatedAt(now);
        if (rule.getId() == null) {
            rule.setCreatedBy(operator.getUserId());
            rule.setCreatedAt(now);
        }
    }

    private void applySnapshot(RuleConfig target, RuleConfigVersion version, AuthenticatedUser operator) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        target.setRuleCode(version.getRuleCode());
        target.setRuleName(version.getRuleName());
        target.setRiskLevel(version.getRiskLevel());
        target.setRiskThreshold(version.getRiskThreshold());
        target.setDurationSeconds(version.getDurationSeconds());
        target.setCooldownSeconds(version.getCooldownSeconds());
        target.setEnabled(version.getEnabled());
        target.setStatus(version.getStatus());
        target.setUpdatedBy(operator.getUserId());
        target.setUpdatedAt(now);
        if (Boolean.TRUE.equals(version.getEnabled())) {
            target.setPublishedAt(now);
            target.setPublishedBy(operator.getUserId());
        } else {
            target.setPublishedAt(null);
            target.setPublishedBy(null);
        }
    }

    private void saveVersionSnapshot(RuleConfig rule,
                                     int versionNo,
                                     String changeSource,
                                     String changeSummary,
                                     AuthenticatedUser operator) {
        RuleConfigVersion version = new RuleConfigVersion();
        version.setRuleConfigId(rule.getId());
        version.setVersionNo(versionNo);
        version.setRuleCode(rule.getRuleCode());
        version.setRuleName(rule.getRuleName());
        version.setRiskLevel(rule.getRiskLevel());
        version.setRiskThreshold(rule.getRiskThreshold());
        version.setDurationSeconds(rule.getDurationSeconds());
        version.setCooldownSeconds(rule.getCooldownSeconds());
        version.setEnabled(rule.getEnabled());
        version.setStatus(rule.getStatus());
        version.setChangeSource(changeSource);
        version.setChangeSummary(normalizeText(changeSummary));
        version.setSnapshotJson(toSnapshotJson(rule, versionNo, changeSource, changeSummary, operator));
        version.setCreatedBy(operator.getUserId());
        version.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        ruleConfigVersionRepository.save(version);
    }

    private String toSnapshotJson(RuleConfig rule,
                                  int versionNo,
                                  String changeSource,
                                  String changeSummary,
                                  AuthenticatedUser operator) {
        try {
            return objectMapper.writeValueAsString(new RuleSnapshot(
                    rule.getId(),
                    rule.getRuleCode(),
                    rule.getRuleName(),
                    rule.getRiskLevel(),
                    rule.getRiskThreshold(),
                    rule.getDurationSeconds(),
                    rule.getCooldownSeconds(),
                    rule.getEnabled(),
                    rule.getStatus(),
                    versionNo,
                    changeSource,
                    normalizeText(changeSummary),
                    operator.getUserId(),
                    LocalDateTime.now(ZoneOffset.UTC)));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "规则版本快照序列化失败");
        }
    }

    private int nextVersion(Long ruleConfigId, Integer currentVersion) {
        return ruleConfigVersionRepository.findFirstByRuleConfigIdOrderByVersionNoDesc(ruleConfigId)
                .map(version -> version.getVersionNo() + 1)
                .orElseGet(() -> currentVersion == null || currentVersion < 1 ? 1 : currentVersion);
    }

    private RuleConfig getRuleOrThrow(Long id) {
        return ruleConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private RuleDefinition toRuleDefinition(RuleConfig rule) {
        return new RuleDefinition(
                rule.getId(),
                rule.getRuleCode(),
                requireRiskLevel(rule.getRiskLevel()),
                rule.getRiskThreshold(),
                rule.getDurationSeconds(),
                rule.getCooldownSeconds(),
                Boolean.TRUE.equals(rule.getEnabled()) && RuleConfigStatus.ENABLED.name().equals(rule.getStatus()));
    }

    private RuleConfigListItemData toListItem(RuleConfig rule) {
        return new RuleConfigListItemData(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                requireRiskLevel(rule.getRiskLevel()).name(),
                rule.getRiskThreshold(),
                rule.getDurationSeconds(),
                rule.getCooldownSeconds(),
                rule.getEnabled(),
                rule.getStatus(),
                rule.getVersion(),
                toOffsetDateTime(rule.getPublishedAt()),
                toOffsetDateTime(rule.getUpdatedAt()),
                ruleAlertCount(rule.getId()),
                ruleFalsePositiveCount(rule.getId()),
                ruleFalsePositiveRate(rule.getId()));
    }

    private RuleConfigDetailData toDetail(RuleConfig rule, List<RuleConfigVersionItemData> versions) {
        return new RuleConfigDetailData(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                requireRiskLevel(rule.getRiskLevel()).name(),
                rule.getRiskThreshold(),
                rule.getDurationSeconds(),
                rule.getCooldownSeconds(),
                rule.getEnabled(),
                rule.getStatus(),
                rule.getVersion(),
                toOffsetDateTime(rule.getPublishedAt()),
                rule.getPublishedBy(),
                toOffsetDateTime(rule.getArchivedAt()),
                rule.getArchivedBy(),
                rule.getCreatedBy(),
                rule.getUpdatedBy(),
                toOffsetDateTime(rule.getCreatedAt()),
                toOffsetDateTime(rule.getUpdatedAt()),
                ruleAlertCount(rule.getId()),
                ruleFalsePositiveCount(rule.getId()),
                ruleFalsePositiveRate(rule.getId()),
                versions);
    }

    private RuleConfigVersionItemData toVersionItem(RuleConfigVersion version) {
        return new RuleConfigVersionItemData(
                version.getId(),
                version.getVersionNo(),
                version.getRuleCode(),
                version.getRuleName(),
                requireRiskLevel(version.getRiskLevel()).name(),
                version.getRiskThreshold(),
                version.getDurationSeconds(),
                version.getCooldownSeconds(),
                version.getEnabled(),
                version.getStatus(),
                version.getChangeSource(),
                version.getChangeSummary(),
                version.getCreatedBy(),
                toOffsetDateTime(version.getCreatedAt()));
    }

    private Long ruleAlertCount(Long ruleId) {
        return alertEventRepository.countByRuleId(ruleId);
    }

    private Long ruleFalsePositiveCount(Long ruleId) {
        return alertEventRepository.countByRuleIdAndStatus(ruleId, AlertStatus.FALSE_POSITIVE.getCode());
    }

    private BigDecimal ruleFalsePositiveRate(Long ruleId) {
        long total = ruleAlertCount(ruleId);
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(ruleFalsePositiveCount(ruleId))
                .divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP);
    }

    private RuleOperationResponseData toOperationResponse(RuleConfig rule, String actionType, String summary) {
        return new RuleOperationResponseData(
                rule.getId(),
                rule.getRuleCode(),
                rule.getVersion(),
                rule.getEnabled(),
                rule.getStatus(),
                actionType,
                toOffsetDateTime(LocalDateTime.now(ZoneOffset.UTC)),
                normalizeText(summary));
    }

    private RiskLevel requireRiskLevel(Integer riskLevelCode) {
        if (riskLevelCode == null) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "规则缺少riskLevel");
        }
        return switch (riskLevelCode) {
            case 1 -> RiskLevel.LOW;
            case 2 -> RiskLevel.MID;
            case 3 -> RiskLevel.HIGH;
            default -> throw new BusinessException(ApiCode.INVALID_PARAM, "不支持的riskLevel");
        };
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateDraftOnlyUpsert(RuleUpsertRequest request) {
        if (Boolean.TRUE.equals(request.enabled()) || request.status() == RuleConfigStatus.ENABLED) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "规则编辑不会直接生效，请使用发布或启停接口");
        }
        if (request.status() == RuleConfigStatus.ARCHIVED) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "不支持通过编辑接口归档规则");
        }
    }

    private RuleConfigStatus resolveEditableStatus(String currentStatus, RuleConfigStatus requestedStatus) {
        if (requestedStatus == null) {
            return RuleConfigStatus.DISABLED.name().equals(currentStatus)
                    ? RuleConfigStatus.DISABLED
                    : RuleConfigStatus.DRAFT;
        }
        return requestedStatus;
    }

    private void ensureNoOtherActiveRuleWithSameRiskLevel(Long currentRuleId, Integer riskLevel) {
        ruleConfigRepository.findAll().stream()
                .filter(rule -> !Objects.equals(rule.getId(), currentRuleId))
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> RuleConfigStatus.ENABLED.name().equals(rule.getStatus()))
                .filter(rule -> Objects.equals(rule.getRiskLevel(), riskLevel))
                .findFirst()
                .ifPresent(conflict -> {
                    throw new BusinessException(
                            ApiCode.INVALID_PARAM,
                            "同一风险等级只允许一条启用规则: " + conflict.getRuleCode());
                });
    }

    private void validateUniqueActiveRiskLevel(List<RuleDefinition> activeRules) {
        Map<Integer, String> seen = new LinkedHashMap<>();
        for (RuleDefinition rule : activeRules) {
            String conflict = seen.putIfAbsent(rule.getRiskLevel().getCode(), rule.getRuleCode());
            if (conflict != null) {
                throw new BusinessException(
                        ApiCode.INTERNAL_ERROR,
                        "存在重复启用的风险等级规则: " + conflict + "," + rule.getRuleCode());
            }
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "page必须大于等于1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "size必须大于等于1");
        }
        return Math.min(size, MAX_SIZE);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private Object buildAuditDetail(RuleConfig rule, String action, String remark) {
        return buildAuditDetail(rule, action, remark, null, null);
    }

    private Object buildAuditDetail(RuleConfig rule, String action, String remark, String extraKey, Object extraValue) {
        java.util.Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("ruleId", rule.getId());
        detail.put("ruleCode", rule.getRuleCode());
        detail.put("ruleName", rule.getRuleName());
        detail.put("version", rule.getVersion());
        detail.put("status", rule.getStatus());
        detail.put("enabled", rule.getEnabled());
        detail.put("action", action);
        detail.put("remark", remark);
        if (extraKey != null) {
            detail.put(extraKey, extraValue);
        }
        return detail;
    }

    private record RuleSnapshot(Long ruleId,
                                String ruleCode,
                                String ruleName,
                                Integer riskLevel,
                                BigDecimal riskThreshold,
                                Integer durationSeconds,
                                Integer cooldownSeconds,
                                Boolean enabled,
                                String status,
                                Integer version,
                                String changeSource,
                                String changeSummary,
                                Long createdBy,
                                LocalDateTime createdAt) {
    }
}
