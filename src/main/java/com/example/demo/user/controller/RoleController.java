package com.example.demo.user.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.user.dto.RoleItemData;
import com.example.demo.user.service.OrgUserManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/roles", "/api/v1/org/users/roles", "/api/v1/platform/enterprise-admins/roles"})
public class RoleController {

    private final OrgUserManagementService orgUserManagementService;

    public RoleController(OrgUserManagementService orgUserManagementService) {
        this.orgUserManagementService = orgUserManagementService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'user.manage')")
    public ApiResponse<List<RoleItemData>> listRoles(Authentication authentication) {
        return ApiResponse.success(orgUserManagementService.listRoles((AuthenticatedUser) authentication.getPrincipal()));
    }
}
