package com.example.demo.user.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.user.dto.RoleItemData;
import com.example.demo.user.service.UserManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final UserManagementService userManagementService;

    public RoleController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'user.manage')")
    public ApiResponse<List<RoleItemData>> listRoles(Authentication authentication) {
        return ApiResponse.success(userManagementService.listRoles((AuthenticatedUser) authentication.getPrincipal()));
    }
}
