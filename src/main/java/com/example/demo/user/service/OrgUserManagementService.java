package com.example.demo.user.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.RoleItemData;
import com.example.demo.user.dto.ResetUserPasswordRequest;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserPageResponseData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgUserManagementService {

    private final UserManagementService userManagementService;

    public OrgUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public UserDetailResponseData createUser(AuthenticatedUser operator, CreateUserRequest request) {
        return userManagementService.createUser(operator, request);
    }

    public UserDetailResponseData updateUser(AuthenticatedUser operator, Long userId, UpdateUserRequest request) {
        return userManagementService.updateUser(operator, userId, request);
    }

    public UserPageResponseData listUsers(AuthenticatedUser operator,
                                          Integer page,
                                          Integer size,
                                          String keyword,
                                          Boolean enabled,
                                          Long enterpriseId) {
        return userManagementService.listUsers(operator, page, size, keyword, enabled, enterpriseId);
    }

    public UserDetailResponseData getUser(AuthenticatedUser operator, Long userId) {
        return userManagementService.getUser(operator, userId);
    }

    public UserDetailResponseData updateRoles(AuthenticatedUser operator, Long userId, List<String> requestedRoles) {
        return userManagementService.updateRoles(operator, userId, requestedRoles);
    }

    public UserDetailResponseData updateStatus(AuthenticatedUser operator, Long userId, Boolean enabled) {
        return userManagementService.updateStatus(operator, userId, enabled);
    }

    public UserDetailResponseData resetPassword(AuthenticatedUser operator, Long userId, ResetUserPasswordRequest request) {
        return userManagementService.resetPassword(operator, userId, request);
    }

    public SystemAuditPageResponseData listUserAudits(AuthenticatedUser operator, Long userId, Integer page, Integer size) {
        return userManagementService.listUserAudits(operator, userId, page, size);
    }

    public List<RoleItemData> listRoles(AuthenticatedUser operator) {
        return userManagementService.listRoles(operator);
    }
}
