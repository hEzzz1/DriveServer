package com.example.demo.auth.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.fleet.repository.FleetRepository;
import org.springframework.stereotype.Service;

@Service
public class BusinessAccessService {

    private final UserAccountRepository userAccountRepository;
    private final UserAuthorizationService userAuthorizationService;
    private final FleetRepository fleetRepository;

    public BusinessAccessService(UserAccountRepository userAccountRepository,
                                 UserAuthorizationService userAuthorizationService,
                                 FleetRepository fleetRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userAuthorizationService = userAuthorizationService;
        this.fleetRepository = fleetRepository;
    }

    public UserAccount getOperatorAccount(AuthenticatedUser operator) {
        return userAccountRepository.findById(operator.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }

    public UserAuthorizationProfile getAuthorizationProfile(AuthenticatedUser operator) {
        return userAuthorizationService.loadProfile(operator);
    }

    public Long resolveReadableEnterpriseId(AuthenticatedUser operator, Long requestedEnterpriseId) {
        if (isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        BusinessDataScope scope = getAuthorizationProfile(operator).dataScope();
        if (requestedEnterpriseId != null) {
            if (!scope.canAccessEnterpriseResource(requestedEnterpriseId)) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            return requestedEnterpriseId;
        }
        if (scope.enterpriseIds().size() == 1) {
            return scope.enterpriseIds().iterator().next();
        }
        throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
    }

    public void assertCanManageEnterprise(AuthenticatedUser operator, Long targetEnterpriseId) {
        if (targetEnterpriseId == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        if (isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (getAuthorizationProfile(operator).dataScope().canAccessEnterpriseResource(targetEnterpriseId)) {
            return;
        }
        throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
    }

    public void assertCanAccessEnterprise(AuthenticatedUser operator, Long enterpriseId) {
        if (isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (enterpriseId == null || !getAuthorizationProfile(operator).dataScope().canAccessEnterpriseResource(enterpriseId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    public void assertCanAccessData(AuthenticatedUser operator, Long enterpriseId, Long fleetId) {
        if (isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (!getAuthorizationProfile(operator).dataScope().canAccessData(enterpriseId, fleetId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    public BusinessDataScope resolveDataScope(AuthenticatedUser operator, Long requestedEnterpriseId, Long requestedFleetId) {
        if (isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        BusinessDataScope scope = getAuthorizationProfile(operator).dataScope();
        if (scope.isEmpty()) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (requestedFleetId != null) {
            Long fleetEnterpriseId = fleetRepository.findById(requestedFleetId)
                    .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"))
                    .getEnterpriseId();
            if (requestedEnterpriseId != null && !requestedEnterpriseId.equals(fleetEnterpriseId)) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            BusinessDataScope restricted = scope.restrictToFleet(requestedFleetId, fleetEnterpriseId);
            if (restricted.isEmpty()) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            return restricted;
        }
        if (requestedEnterpriseId != null) {
            BusinessDataScope restricted = scope.restrictToEnterprise(requestedEnterpriseId);
            if (restricted.isEmpty()) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            return restricted;
        }
        return scope;
    }

    public Long requireOperatorEnterpriseId(AuthenticatedUser operator) {
        UserAccount currentUser = getOperatorAccount(operator);
        if (currentUser.getEnterpriseId() == null) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return currentUser.getEnterpriseId();
    }

    public void assertPlatformAdmin(AuthenticatedUser operator) {
        if (!isPlatformAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    public boolean isSuperAdmin(AuthenticatedUser operator) {
        return getAuthorizationProfile(operator).hasPlatformRole(com.example.demo.auth.model.RoleTemplateCode.PLATFORM_SUPER_ADMIN.name());
    }

    public boolean isPlatformAdmin(AuthenticatedUser operator) {
        return !getAuthorizationProfile(operator).platformRoles().isEmpty();
    }

    public boolean hasPermission(AuthenticatedUser operator, String permission) {
        return getAuthorizationProfile(operator).hasPermission(permission);
    }
}
