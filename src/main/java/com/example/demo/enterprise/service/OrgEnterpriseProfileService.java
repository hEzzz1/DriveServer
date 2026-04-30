package com.example.demo.enterprise.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.dto.EnterpriseActivationCodeResponseData;
import com.example.demo.enterprise.dto.EnterpriseDetailResponseData;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogPageResponseData;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class OrgEnterpriseProfileService {

    private final EnterpriseRepository enterpriseRepository;
    private final BusinessAccessService businessAccessService;
    private final EnterpriseActivationCodeService enterpriseActivationCodeService;
    private final com.example.demo.device.service.EnterpriseDeviceBindLogService enterpriseDeviceBindLogService;

    public OrgEnterpriseProfileService(EnterpriseRepository enterpriseRepository,
                                       BusinessAccessService businessAccessService,
                                       EnterpriseActivationCodeService enterpriseActivationCodeService,
                                       com.example.demo.device.service.EnterpriseDeviceBindLogService enterpriseDeviceBindLogService) {
        this.enterpriseRepository = enterpriseRepository;
        this.businessAccessService = businessAccessService;
        this.enterpriseActivationCodeService = enterpriseActivationCodeService;
        this.enterpriseDeviceBindLogService = enterpriseDeviceBindLogService;
    }

    @Transactional(readOnly = true)
    public EnterpriseDetailResponseData getProfile(AuthenticatedUser operator) {
        Enterprise enterprise = getCurrentEnterprise(operator);
        return new EnterpriseDetailResponseData(
                enterprise.getId(),
                enterprise.getCode(),
                enterprise.getName(),
                enterprise.getStatus(),
                enterprise.getRemark(),
                enterprise.getCreatedAt() == null ? null : enterprise.getCreatedAt().atOffset(ZoneOffset.UTC),
                enterprise.getUpdatedAt() == null ? null : enterprise.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public EnterpriseActivationCodeResponseData getActivationCode(AuthenticatedUser operator) {
        return enterpriseActivationCodeService.getActivationCode(operator, currentEnterpriseId(operator));
    }

    @Transactional
    public EnterpriseActivationCodeResponseData rotateActivationCode(AuthenticatedUser operator) {
        return enterpriseActivationCodeService.rotateActivationCode(operator, currentEnterpriseId(operator));
    }

    @Transactional
    public EnterpriseActivationCodeResponseData disableActivationCode(AuthenticatedUser operator) {
        return enterpriseActivationCodeService.disableActivationCode(operator, currentEnterpriseId(operator));
    }

    @Transactional(readOnly = true)
    public EnterpriseDeviceBindLogPageResponseData listDeviceBindLogs(AuthenticatedUser operator, Integer page, Integer size) {
        return enterpriseDeviceBindLogService.list(operator, currentEnterpriseId(operator), page, size);
    }

    private Long currentEnterpriseId(AuthenticatedUser operator) {
        return businessAccessService.requireOperatorEnterpriseId(operator);
    }

    private Enterprise getCurrentEnterprise(AuthenticatedUser operator) {
        return enterpriseRepository.findById(currentEnterpriseId(operator))
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }
}
