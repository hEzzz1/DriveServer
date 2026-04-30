package com.example.demo.system.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.trace.TraceIdContext;
import com.example.demo.system.dto.SystemAuditDetailData;
import com.example.demo.system.dto.SystemAuditExportResponseData;
import com.example.demo.system.dto.SystemAuditListItemData;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.system.entity.SystemAuditLog;
import com.example.demo.system.repository.SystemAuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SystemAuditService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;
    private static final Set<String> PLATFORM_GOVERNANCE_MODULES = Set.of("SYSTEM", "AUDIT", "RULE");
    private static final Set<String> PLATFORM_GOVERNANCE_ACTION_TYPES = Set.of(
            "CREATE_ENTERPRISE",
            "UPDATE_ENTERPRISE",
            "UPDATE_ENTERPRISE_STATUS",
            "CREATE_ENTERPRISE_ADMIN",
            "UPDATE_ENTERPRISE_ADMIN_PROFILE",
            "UPDATE_ENTERPRISE_ADMIN_ROLES",
            "UPDATE_ENTERPRISE_ADMIN_STATUS",
            "RESET_ENTERPRISE_ADMIN_PASSWORD",
            "CREATE_INTERNAL_USER",
            "UPDATE_INTERNAL_USER_PROFILE",
            "UPDATE_INTERNAL_USER_ROLES",
            "UPDATE_INTERNAL_USER_STATUS",
            "RESET_INTERNAL_USER_PASSWORD");

    private final SystemAuditRepository systemAuditRepository;
    private final UserAccountRepository userAccountRepository;
    private final BusinessAccessService businessAccessService;
    private final ObjectMapper objectMapper;

    public SystemAuditService(SystemAuditRepository systemAuditRepository,
                              UserAccountRepository userAccountRepository,
                              BusinessAccessService businessAccessService,
                              ObjectMapper objectMapper) {
        this.systemAuditRepository = systemAuditRepository;
        this.userAccountRepository = userAccountRepository;
        this.businessAccessService = businessAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SystemAuditLog record(AuthenticatedUser operator,
                                 String module,
                                 String actionType,
                                 String targetType,
                                 String targetId,
                                 String actionResult,
                                 String actionRemark,
                                 Object detail) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        HttpServletRequest request = currentRequest();
        String traceId = TraceIdContext.getTraceId();
        Long operatorEnterpriseId = resolveOperatorEnterpriseId(operator);
        Long targetEnterpriseId = resolveTargetEnterpriseId(detail, targetType, targetId);

        SystemAuditLog auditLog = new SystemAuditLog();
        auditLog.setOperatorId(operator == null ? null : operator.getUserId());
        auditLog.setOperatorEnterpriseId(operatorEnterpriseId);
        auditLog.setOperatorName(operator == null ? "system" : operator.getUsername());
        auditLog.setModule(normalize(module, "SYSTEM"));
        auditLog.setAction(normalize(actionType, "UNKNOWN"));
        auditLog.setTargetId(normalize(targetId, null));
        auditLog.setDetailJson(toJson(detail));
        auditLog.setIp(request == null ? null : request.getRemoteAddr());
        auditLog.setActionType(normalize(actionType, "UNKNOWN"));
        auditLog.setActionBy(operator == null ? null : operator.getUserId());
        auditLog.setActionTime(now);
        auditLog.setActionTargetType(normalize(targetType, "UNKNOWN"));
        auditLog.setActionTargetId(normalize(targetId, null));
        auditLog.setTargetEnterpriseId(targetEnterpriseId);
        auditLog.setActionResult(normalize(actionResult, "SUCCESS"));
        auditLog.setActionRemark(normalize(actionRemark, null));
        auditLog.setTraceId(normalize(traceId, null));
        auditLog.setUserAgent(request == null ? null : normalize(request.getHeader("User-Agent"), null));
        auditLog.setCreatedAt(now);
        return systemAuditRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public SystemAuditPageResponseData list(String module,
                                           String actionType,
                                           String targetType,
                                           String targetId,
                                           Long actionBy,
                                           OffsetDateTime startTime,
                                           OffsetDateTime endTime,
                                           Integer page,
                                           Integer size) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        if (startUtc != null && endUtc != null && startUtc.isAfter(endUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }

        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc, null);
        Page<SystemAuditLog> pageResult = systemAuditRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<SystemAuditListItemData> items = pageResult.getContent().stream().map(this::toListItem).toList();
        return new SystemAuditPageResponseData(pageResult.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public SystemAuditDetailData getDetail(Long id) {
        SystemAuditLog log = getLog(id);
        return new SystemAuditDetailData(
                log.getId(),
                log.getOperatorId(),
                log.getOperatorName(),
                log.getModule(),
                log.getAction(),
                log.getTargetId(),
                log.getDetailJson(),
                log.getActionType(),
                log.getActionBy(),
                toOffsetDateTime(log.getActionTime()),
                log.getActionTargetType(),
                log.getActionTargetId(),
                log.getActionResult(),
                log.getActionRemark(),
                log.getTraceId(),
                log.getIp(),
                log.getUserAgent(),
                toOffsetDateTime(log.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public SystemAuditExportResponseData export(String module,
                                               String actionType,
                                               String targetType,
                                               String targetId,
                                               Long actionBy,
                                               OffsetDateTime startTime,
                                               OffsetDateTime endTime) {
        List<SystemAuditListItemData> items = listAll(module, actionType, targetType, targetId, actionBy, startTime, endTime)
                .stream()
                .map(this::toListItem)
                .toList();
        return new SystemAuditExportResponseData(OffsetDateTime.now(ZoneOffset.UTC), items.size(), items);
    }

    @Transactional(readOnly = true)
    public List<SystemAuditLog> listAll(String module,
                                        String actionType,
                                        String targetType,
                                        String targetId,
                                        Long actionBy,
                                        OffsetDateTime startTime,
                                        OffsetDateTime endTime) {
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc, null);
        return systemAuditRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional(readOnly = true)
    public SystemAuditPageResponseData listForPlatform(AuthenticatedUser operator,
                                                       String module,
                                                       String actionType,
                                                       String targetType,
                                                       String targetId,
                                                       Long actionBy,
                                                       OffsetDateTime startTime,
                                                       OffsetDateTime endTime,
                                                       Integer page,
                                                       Integer size) {
        businessAccessService.assertPlatformAdmin(operator);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        if (startUtc != null && endUtc != null && startUtc.isAfter(endUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }
        Specification<SystemAuditLog> specification = buildPlatformGovernanceSpecification(
                module, actionType, targetType, targetId, actionBy, startUtc, endUtc);
        Page<SystemAuditLog> pageResult = systemAuditRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<SystemAuditListItemData> items = pageResult.getContent().stream().map(this::toListItem).toList();
        return new SystemAuditPageResponseData(pageResult.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public SystemAuditDetailData getDetailForPlatform(AuthenticatedUser operator, Long id) {
        businessAccessService.assertPlatformAdmin(operator);
        SystemAuditLog log = getLog(id);
        if (!isPlatformGovernanceLog(log)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return toDetail(log);
    }

    @Transactional(readOnly = true)
    public SystemAuditExportResponseData exportForPlatform(AuthenticatedUser operator,
                                                           String module,
                                                           String actionType,
                                                           String targetType,
                                                           String targetId,
                                                           Long actionBy,
                                                           OffsetDateTime startTime,
                                                           OffsetDateTime endTime) {
        businessAccessService.assertPlatformAdmin(operator);
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        if (startUtc != null && endUtc != null && startUtc.isAfter(endUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }
        List<SystemAuditListItemData> items = systemAuditRepository.findAll(
                        buildPlatformGovernanceSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc),
                        Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(this::toListItem)
                .toList();
        return new SystemAuditExportResponseData(OffsetDateTime.now(ZoneOffset.UTC), items.size(), items);
    }

    @Transactional(readOnly = true)
    public SystemAuditPageResponseData listForOrg(AuthenticatedUser operator,
                                                  String module,
                                                  String actionType,
                                                  String targetType,
                                                  String targetId,
                                                  Long actionBy,
                                                  OffsetDateTime startTime,
                                                  OffsetDateTime endTime,
                                                  Integer page,
                                                  Integer size) {
        Long enterpriseId = requireOrgEnterpriseId(operator);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        if (startUtc != null && endUtc != null && startUtc.isAfter(endUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }

        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc, enterpriseId);
        Page<SystemAuditLog> pageResult = systemAuditRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<SystemAuditListItemData> items = pageResult.getContent().stream().map(this::toListItem).toList();
        return new SystemAuditPageResponseData(pageResult.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public SystemAuditDetailData getDetailForOrg(AuthenticatedUser operator, Long id) {
        Long enterpriseId = requireOrgEnterpriseId(operator);
        SystemAuditLog log = getLog(id);
        if (!belongsToEnterprise(log, enterpriseId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return toDetail(log);
    }

    @Transactional(readOnly = true)
    public SystemAuditExportResponseData exportForOrg(AuthenticatedUser operator,
                                                      String module,
                                                      String actionType,
                                                      String targetType,
                                                      String targetId,
                                                      Long actionBy,
                                                      OffsetDateTime startTime,
                                                      OffsetDateTime endTime) {
        Long enterpriseId = requireOrgEnterpriseId(operator);
        LocalDateTime startUtc = startTime == null ? null : LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = endTime == null ? null : LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.UTC);
        if (startUtc != null && endUtc != null && startUtc.isAfter(endUtc)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "startTime不能晚于endTime");
        }
        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc, enterpriseId);
        List<SystemAuditListItemData> items = systemAuditRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(this::toListItem)
                .toList();
        return new SystemAuditExportResponseData(OffsetDateTime.now(ZoneOffset.UTC), items.size(), items);
    }

    private Specification<SystemAuditLog> buildSpecification(String module,
                                                             String actionType,
                                                             String targetType,
                                                             String targetId,
                                                             Long actionBy,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime,
                                                             Long enterpriseId) {
        List<Specification<SystemAuditLog>> specifications = new ArrayList<>();
        if (StringUtils.hasText(module)) {
            specifications.add((root, query, cb) -> cb.equal(root.get("module"), module.trim()));
        }
        if (StringUtils.hasText(actionType)) {
            specifications.add((root, query, cb) -> cb.equal(root.get("actionType"), actionType.trim()));
        }
        if (StringUtils.hasText(targetType)) {
            specifications.add((root, query, cb) -> cb.equal(root.get("actionTargetType"), targetType.trim()));
        }
        if (StringUtils.hasText(targetId)) {
            specifications.add((root, query, cb) -> cb.equal(root.get("actionTargetId"), targetId.trim()));
        }
        if (actionBy != null) {
            specifications.add((root, query, cb) -> cb.equal(root.get("actionBy"), actionBy));
        }
        if (startTime != null) {
            specifications.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("actionTime"), startTime));
        }
        if (endTime != null) {
            specifications.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("actionTime"), endTime));
        }
        if (enterpriseId != null) {
            specifications.add((root, query, cb) -> cb.or(
                    cb.equal(root.get("operatorEnterpriseId"), enterpriseId),
                    cb.equal(root.get("targetEnterpriseId"), enterpriseId)));
        }
        return specifications.stream().reduce(Specification.where(null), Specification::and);
    }

    private Specification<SystemAuditLog> buildPlatformGovernanceSpecification(String module,
                                                                               String actionType,
                                                                               String targetType,
                                                                               String targetId,
                                                                               Long actionBy,
                                                                               LocalDateTime startTime,
                                                                               LocalDateTime endTime) {
        return buildSpecification(module, actionType, targetType, targetId, actionBy, startTime, endTime, null)
                .and((root, query, cb) -> cb.or(
                        root.get("module").in(PLATFORM_GOVERNANCE_MODULES),
                        root.get("actionType").in(PLATFORM_GOVERNANCE_ACTION_TYPES)));
    }

    private SystemAuditDetailData toDetail(SystemAuditLog log) {
        return new SystemAuditDetailData(
                log.getId(),
                log.getOperatorId(),
                log.getOperatorName(),
                log.getModule(),
                log.getAction(),
                log.getTargetId(),
                log.getDetailJson(),
                log.getActionType(),
                log.getActionBy(),
                toOffsetDateTime(log.getActionTime()),
                log.getActionTargetType(),
                log.getActionTargetId(),
                log.getActionResult(),
                log.getActionRemark(),
                log.getTraceId(),
                log.getIp(),
                log.getUserAgent(),
                toOffsetDateTime(log.getCreatedAt()));
    }

    private SystemAuditLog getLog(Long id) {
        return systemAuditRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private Long requireOrgEnterpriseId(AuthenticatedUser operator) {
        if (businessAccessService.isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return businessAccessService.requireOperatorEnterpriseId(operator);
    }

    private boolean belongsToEnterprise(SystemAuditLog log, Long enterpriseId) {
        return enterpriseId != null && (enterpriseId.equals(log.getOperatorEnterpriseId()) || enterpriseId.equals(log.getTargetEnterpriseId()));
    }

    private boolean isPlatformGovernanceLog(SystemAuditLog log) {
        if (log == null) {
            return false;
        }
        return PLATFORM_GOVERNANCE_MODULES.contains(log.getModule())
                || PLATFORM_GOVERNANCE_ACTION_TYPES.contains(log.getActionType());
    }

    private Long resolveOperatorEnterpriseId(AuthenticatedUser operator) {
        if (operator == null || operator.getUserId() == null) {
            return null;
        }
        return userAccountRepository.findById(operator.getUserId())
                .map(UserAccount::getEnterpriseId)
                .orElse(null);
    }

    private Long resolveTargetEnterpriseId(Object detail, String targetType, String targetId) {
        Long fromDetail = extractEnterpriseId(detail, "targetEnterpriseId");
        if (fromDetail != null) {
            return fromDetail;
        }
        Long genericEnterpriseId = extractEnterpriseId(detail, "enterpriseId");
        if (genericEnterpriseId != null) {
            return genericEnterpriseId;
        }
        if ("ENTERPRISE".equalsIgnoreCase(normalize(targetType, null))) {
            return parseLong(targetId);
        }
        return null;
    }

    private Long extractEnterpriseId(Object detail, String fieldName) {
        if (detail == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode node = objectMapper.valueToTree(detail).path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.canConvertToLong()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            return parseLong(node.asText());
        }
        return null;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private SystemAuditListItemData toListItem(SystemAuditLog log) {
        return new SystemAuditListItemData(
                log.getId(),
                log.getOperatorId(),
                log.getOperatorName(),
                log.getModule(),
                log.getAction(),
                log.getTargetId(),
                log.getActionType(),
                log.getActionBy(),
                toOffsetDateTime(log.getActionTime()),
                log.getActionTargetType(),
                log.getActionTargetId(),
                log.getActionResult(),
                log.getActionRemark(),
                log.getTraceId(),
                log.getIp(),
                log.getUserAgent());
    }

    private String normalize(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getClass().getSimpleName() + "\"}";
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

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }
}
