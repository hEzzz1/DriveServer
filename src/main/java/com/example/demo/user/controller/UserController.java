package com.example.demo.user.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.EnterpriseAdminOrSuperAdmin;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.ResetUserPasswordRequest;
import com.example.demo.user.dto.UpdateUserRolesRequest;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UpdateUserStatusRequest;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserPageResponseData;
import com.example.demo.user.service.UserManagementService;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> createUser(@Valid @RequestBody CreateUserRequest request,
                                                          Authentication authentication) {
        return ApiResponse.success(userManagementService.createUser(currentUser(authentication), request));
    }

    @GetMapping
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserPageResponseData> listUsers(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Boolean enabled,
                                                       @RequestParam(required = false) Long enterpriseId,
                                                       Authentication authentication) {
        return ApiResponse.success(userManagementService.listUsers(currentUser(authentication), page, size, keyword, enabled, enterpriseId));
    }

    @GetMapping("/{id}")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> getUser(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(userManagementService.getUser(currentUser(authentication), id));
    }

    @PutMapping("/{id}")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> updateUser(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateUserRequest request,
                                                          Authentication authentication) {
        return ApiResponse.success(userManagementService.updateUser(currentUser(authentication), id, request));
    }

    @RequestMapping(path = "/{id}/roles", method = {RequestMethod.PUT, RequestMethod.POST})
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> updateRoles(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateUserRolesRequest request,
                                                           Authentication authentication) {
        return ApiResponse.success(userManagementService.updateRoles(currentUser(authentication), id, request.roles()));
    }

    @PutMapping("/{id}/status")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserStatusRequest request,
                                                            Authentication authentication) {
        return ApiResponse.success(userManagementService.updateStatus(currentUser(authentication), id, request.enabled()));
    }

    @PostMapping("/{id}/reset-password")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<UserDetailResponseData> resetPassword(@PathVariable Long id,
                                                             @Valid @RequestBody ResetUserPasswordRequest request,
                                                             Authentication authentication) {
        return ApiResponse.success(userManagementService.resetPassword(currentUser(authentication), id, request));
    }

    @GetMapping("/{id}/audits")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<SystemAuditPageResponseData> listUserAudits(@PathVariable Long id,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size,
                                                                   Authentication authentication) {
        return ApiResponse.success(userManagementService.listUserAudits(currentUser(authentication), id, page, size));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
