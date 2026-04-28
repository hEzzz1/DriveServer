package com.example.demo.auth.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.RoleCode;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class BusinessAccessService {

    private final UserAccountRepository userAccountRepository;

    public BusinessAccessService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount getOperatorAccount(AuthenticatedUser operator) {
        return userAccountRepository.findById(operator.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }

    public Long resolveReadableEnterpriseId(AuthenticatedUser operator, Long requestedEnterpriseId) {
        if (isSuperAdmin(operator)) {
            return requestedEnterpriseId;
        }
        if (hasAnyRole(operator, RoleCode.ENTERPRISE_ADMIN, RoleCode.OPERATOR, RoleCode.ANALYST)) {
            Long currentEnterpriseId = requireOperatorEnterpriseId(operator);
            if (requestedEnterpriseId != null && !requestedEnterpriseId.equals(currentEnterpriseId)) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            return currentEnterpriseId;
        }
        throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
    }

    public void assertCanManageEnterprise(AuthenticatedUser operator, Long targetEnterpriseId) {
        if (targetEnterpriseId == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        if (isSuperAdmin(operator)) {
            return;
        }
        if (isEnterpriseAdmin(operator) && targetEnterpriseId.equals(requireOperatorEnterpriseId(operator))) {
            return;
        }
        throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
    }

    public Long requireOperatorEnterpriseId(AuthenticatedUser operator) {
        UserAccount currentUser = getOperatorAccount(operator);
        if (currentUser.getEnterpriseId() == null) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return currentUser.getEnterpriseId();
    }

    public boolean isSuperAdmin(AuthenticatedUser operator) {
        return operator.getRoles().contains(RoleCode.SUPER_ADMIN.name());
    }

    public boolean isEnterpriseAdmin(AuthenticatedUser operator) {
        return operator.getRoles().contains(RoleCode.ENTERPRISE_ADMIN.name()) && !isSuperAdmin(operator);
    }

    private boolean hasAnyRole(AuthenticatedUser operator, RoleCode... roleCodes) {
        for (RoleCode roleCode : roleCodes) {
            if (operator.getRoles().contains(roleCode.name())) {
                return true;
            }
        }
        return false;
    }
}
