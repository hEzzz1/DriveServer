package com.example.demo.system.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.trace.TraceIdContext;
import com.example.demo.system.dto.SystemErrorTraceItemData;
import com.example.demo.system.dto.SystemErrorTracePageResponseData;
import com.example.demo.system.entity.SystemErrorTrace;
import com.example.demo.system.repository.SystemErrorTraceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class SystemDiagnosticsService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SystemErrorTraceRepository systemErrorTraceRepository;
    private final BusinessAccessService businessAccessService;

    public SystemDiagnosticsService(SystemErrorTraceRepository systemErrorTraceRepository,
                                    BusinessAccessService businessAccessService) {
        this.systemErrorTraceRepository = systemErrorTraceRepository;
        this.businessAccessService = businessAccessService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, ApiCode apiCode, String message, Exception exception) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        SystemErrorTrace trace = new SystemErrorTrace();
        trace.setTraceId(normalize(TraceIdContext.getTraceId(), "", 64));
        trace.setOccurredAt(now);
        trace.setMethod(request == null ? null : normalize(request.getMethod(), null, 16));
        trace.setRequestPath(request == null ? null : normalize(request.getRequestURI(), null, 512));
        trace.setQueryString(request == null ? null : normalize(request.getQueryString(), null, 1024));
        trace.setHttpStatus(apiCode.getHttpStatus().value());
        trace.setCode(apiCode.getCode());
        trace.setMessage(normalize(message, apiCode.getMessage(), 255));
        trace.setExceptionClass(exception == null ? null : normalize(exception.getClass().getName(), null, 255));
        trace.setSummary(buildSummary(exception));
        trace.setOperatorId(resolveOperatorId());
        trace.setIp(request == null ? null : normalize(request.getRemoteAddr(), null, 64));
        trace.setUserAgent(request == null ? null : normalize(request.getHeader("User-Agent"), null, 255));
        trace.setCreatedAt(now);
        systemErrorTraceRepository.save(trace);
    }

    @Transactional(readOnly = true)
    public SystemErrorTracePageResponseData list(AuthenticatedUser operator, String traceId, Integer page, Integer size) {
        businessAccessService.assertPlatformAdmin(operator);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<SystemErrorTrace> specification = buildSpecification(traceId);
        Page<SystemErrorTrace> result = systemErrorTraceRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new SystemErrorTracePageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(this::toItem).toList());
    }

    @Transactional(readOnly = true)
    public SystemErrorTraceItemData getByTraceId(AuthenticatedUser operator, String traceId) {
        businessAccessService.assertPlatformAdmin(operator);
        String normalizedTraceId = normalizeRequired(traceId, "traceId不能为空");
        return systemErrorTraceRepository.findFirstByTraceIdOrderByOccurredAtDescIdDesc(normalizedTraceId)
                .map(this::toItem)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, "未找到对应追踪记录"));
    }

    private Specification<SystemErrorTrace> buildSpecification(String traceId) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(traceId)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("traceId"), traceId.trim());
        };
    }

    private SystemErrorTraceItemData toItem(SystemErrorTrace trace) {
        return new SystemErrorTraceItemData(
                trace.getId(),
                trace.getTraceId(),
                toOffsetDateTime(trace.getOccurredAt()),
                trace.getMethod(),
                trace.getRequestPath(),
                trace.getQueryString(),
                trace.getHttpStatus(),
                trace.getCode(),
                trace.getMessage(),
                trace.getExceptionClass(),
                trace.getSummary(),
                trace.getOperatorId(),
                trace.getIp(),
                trace.getUserAgent());
    }

    private Long resolveOperatorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof AuthenticatedUser user ? user.getUserId() : null;
    }

    private String buildSummary(Exception exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return null;
        }
        return normalize(exception.getMessage().replaceAll("\\s+", " "), null, 1000);
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

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, message);
        }
        return value.trim();
    }

    private String normalize(String value, String fallback, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
