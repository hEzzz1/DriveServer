package com.example.demo.system.service;

import com.example.demo.auth.security.AuthenticatedUser;
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

@Service
public class SystemAuditService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final SystemAuditRepository systemAuditRepository;
    private final ObjectMapper objectMapper;

    public SystemAuditService(SystemAuditRepository systemAuditRepository, ObjectMapper objectMapper) {
        this.systemAuditRepository = systemAuditRepository;
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

        SystemAuditLog auditLog = new SystemAuditLog();
        auditLog.setOperatorId(operator == null ? 0L : operator.getUserId());
        auditLog.setOperatorName(operator == null ? "system" : operator.getUsername());
        auditLog.setModule(normalize(module, "SYSTEM"));
        auditLog.setAction(normalize(actionType, "UNKNOWN"));
        auditLog.setTargetId(normalize(targetId, null));
        auditLog.setDetailJson(toJson(detail));
        auditLog.setIp(request == null ? null : request.getRemoteAddr());
        auditLog.setActionType(normalize(actionType, "UNKNOWN"));
        auditLog.setActionBy(operator == null ? 0L : operator.getUserId());
        auditLog.setActionTime(now);
        auditLog.setActionTargetType(normalize(targetType, "UNKNOWN"));
        auditLog.setActionTargetId(normalize(targetId, null));
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

        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc);
        Page<SystemAuditLog> pageResult = systemAuditRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<SystemAuditListItemData> items = pageResult.getContent().stream().map(this::toListItem).toList();
        return new SystemAuditPageResponseData(pageResult.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public SystemAuditDetailData getDetail(Long id) {
        SystemAuditLog log = systemAuditRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
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
        Specification<SystemAuditLog> specification = buildSpecification(module, actionType, targetType, targetId, actionBy, startUtc, endUtc);
        return systemAuditRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "actionTime").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private Specification<SystemAuditLog> buildSpecification(String module,
                                                             String actionType,
                                                             String targetType,
                                                             String targetId,
                                                             Long actionBy,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
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
        return specifications.stream().reduce(Specification.where(null), Specification::and);
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
