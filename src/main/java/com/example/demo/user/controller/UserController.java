package com.example.demo.user.controller;

import com.example.demo.auth.security.SuperAdminOnly;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.user.dto.UpdateUserRolesRequest;
import com.example.demo.user.dto.UpdateUserStatusRequest;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserPageResponseData;
import com.example.demo.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @SuperAdminOnly
    public ApiResponse<UserPageResponseData> listUsers(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(userManagementService.listUsers(page, size, keyword, enabled));
    }

    @GetMapping("/{id}")
    @SuperAdminOnly
    public ApiResponse<UserDetailResponseData> getUser(@PathVariable Long id) {
        return ApiResponse.success(userManagementService.getUser(id));
    }

    @PutMapping("/{id}/roles")
    @SuperAdminOnly
    public ApiResponse<UserDetailResponseData> updateRoles(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.success(userManagementService.updateRoles(id, request.roles()));
    }

    @PutMapping("/{id}/status")
    @SuperAdminOnly
    public ApiResponse<UserDetailResponseData> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success(userManagementService.updateStatus(id, request.enabled()));
    }
}
