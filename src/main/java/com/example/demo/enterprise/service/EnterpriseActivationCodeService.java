package com.example.demo.enterprise.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.dto.EnterpriseActivationCodeResponseData;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.model.EnterpriseActivationCodeStatus;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.system.service.SystemAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EnterpriseActivationCodeService {

    private static final String CODE_PREFIX = "ENT-";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EnterpriseRepository enterpriseRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public EnterpriseActivationCodeService(EnterpriseRepository enterpriseRepository,
                                           BusinessAccessService businessAccessService,
                                           SystemAuditService systemAuditService) {
        this.enterpriseRepository = enterpriseRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
    }

    public void initializeForNewEnterprise(Enterprise enterprise, LocalDateTime now) {
        enterprise.setActivationCode(generateUniqueCode());
        enterprise.setActivationCodeStatus(EnterpriseActivationCodeStatus.ACTIVE.name());
        enterprise.setActivationCodeRotatedAt(now);
        enterprise.setActivationCodeExpiresAt(null);
        enterprise.setActivationCodeRemark(null);
    }

    @Transactional(readOnly = true)
    public EnterpriseActivationCodeResponseData getActivationCode(AuthenticatedUser operator, Long enterpriseId) {
        Enterprise enterprise = getManageableEnterprise(operator, enterpriseId);
        return toResponse(enterprise);
    }

    @Transactional
    public EnterpriseActivationCodeResponseData rotateActivationCode(AuthenticatedUser operator, Long enterpriseId) {
        Enterprise enterprise = getManageableEnterprise(operator, enterpriseId);
        String before = enterprise.getActivationCode();
        enterprise.setActivationCode(generateUniqueCode());
        enterprise.setActivationCodeStatus(EnterpriseActivationCodeStatus.ACTIVE.name());
        enterprise.setActivationCodeRotatedAt(LocalDateTime.now(ZoneOffset.UTC));
        enterprise.setActivationCodeExpiresAt(null);
        Enterprise saved = enterpriseRepository.save(enterprise);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("enterpriseId", saved.getId());
        detail.put("beforeMasked", maskActivationCode(before));
        detail.put("afterMasked", maskActivationCode(saved.getActivationCode()));
        systemAuditService.record(operator, "ENTERPRISE", "ROTATE_ENTERPRISE_ACTIVATION_CODE", "ENTERPRISE",
                String.valueOf(saved.getId()), "SUCCESS", "轮换企业激活码", detail);
        return toResponse(saved);
    }

    @Transactional
    public EnterpriseActivationCodeResponseData disableActivationCode(AuthenticatedUser operator, Long enterpriseId) {
        Enterprise enterprise = getManageableEnterprise(operator, enterpriseId);
        enterprise.setActivationCodeStatus(EnterpriseActivationCodeStatus.DISABLED.name());
        Enterprise saved = enterpriseRepository.save(enterprise);
        systemAuditService.record(operator, "ENTERPRISE", "DISABLE_ENTERPRISE_ACTIVATION_CODE", "ENTERPRISE",
                String.valueOf(saved.getId()), "SUCCESS", "停用企业激活码",
                Map.of("enterpriseId", saved.getId(), "activationCodeMasked", maskActivationCode(saved.getActivationCode())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Enterprise resolveActiveEnterprise(String activationCode) {
        String normalizedCode = normalizeRequired(activationCode, "enterpriseActivationCode不能为空");
        Enterprise enterprise = enterpriseRepository.findByActivationCode(normalizedCode)
                .orElseThrow(() -> new BusinessException(ApiCode.ENTERPRISE_ACTIVATION_CODE_NOT_FOUND, ApiCode.ENTERPRISE_ACTIVATION_CODE_NOT_FOUND.getMessage()));
        EnterpriseActivationCodeStatus resolvedStatus = resolveStatus(enterprise);
        if (resolvedStatus == EnterpriseActivationCodeStatus.EXPIRED) {
            throw new BusinessException(ApiCode.ENTERPRISE_ACTIVATION_CODE_EXPIRED, ApiCode.ENTERPRISE_ACTIVATION_CODE_EXPIRED.getMessage());
        }
        if (resolvedStatus == EnterpriseActivationCodeStatus.DISABLED) {
            throw new BusinessException(ApiCode.ENTERPRISE_ACTIVATION_CODE_DISABLED, ApiCode.ENTERPRISE_ACTIVATION_CODE_DISABLED.getMessage());
        }
        if (enterprise.getStatus() == null || enterprise.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.FORBIDDEN, "企业已禁用");
        }
        return enterprise;
    }

    public String maskActivationCode(String activationCode) {
        if (!StringUtils.hasText(activationCode)) {
            return null;
        }
        String trimmed = activationCode.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 5);
    }

    public EnterpriseActivationCodeStatus resolveStatus(Enterprise enterprise) {
        if (enterprise == null) {
            return EnterpriseActivationCodeStatus.DISABLED;
        }
        if (enterprise.getActivationCodeExpiresAt() != null
                && enterprise.getActivationCodeExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            return EnterpriseActivationCodeStatus.EXPIRED;
        }
        if (!StringUtils.hasText(enterprise.getActivationCodeStatus())) {
            return EnterpriseActivationCodeStatus.ACTIVE;
        }
        try {
            return EnterpriseActivationCodeStatus.valueOf(enterprise.getActivationCodeStatus());
        } catch (IllegalArgumentException ex) {
            return EnterpriseActivationCodeStatus.DISABLED;
        }
    }

    private Enterprise getManageableEnterprise(AuthenticatedUser operator, Long enterpriseId) {
        businessAccessService.assertCanManageEnterprise(operator, enterpriseId);
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private EnterpriseActivationCodeResponseData toResponse(Enterprise enterprise) {
        return new EnterpriseActivationCodeResponseData(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getActivationCode(),
                maskActivationCode(enterprise.getActivationCode()),
                resolveStatus(enterprise).name(),
                toOffsetDateTime(enterprise.getActivationCodeRotatedAt()),
                toOffsetDateTime(enterprise.getActivationCodeExpiresAt()));
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = CODE_PREFIX + randomBlock(4) + "-" + randomBlock(4);
            if (!enterpriseRepository.existsByActivationCode(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ApiCode.INTERNAL_ERROR, "无法生成唯一企业激活码");
    }

    private String randomBlock(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, message);
        }
        return value.trim();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
