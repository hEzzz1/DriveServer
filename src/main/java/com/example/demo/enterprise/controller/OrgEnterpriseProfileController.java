package com.example.demo.enterprise.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.enterprise.dto.EnterpriseActivationCodeResponseData;
import com.example.demo.enterprise.dto.EnterpriseDetailResponseData;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogPageResponseData;
import com.example.demo.enterprise.service.OrgEnterpriseProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/enterprise-profile")
public class OrgEnterpriseProfileController {

    private final OrgEnterpriseProfileService orgEnterpriseProfileService;

    public OrgEnterpriseProfileController(OrgEnterpriseProfileService orgEnterpriseProfileService) {
        this.orgEnterpriseProfileService = orgEnterpriseProfileService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.read')")
    public ApiResponse<EnterpriseDetailResponseData> getProfile(Authentication authentication) {
        return ApiResponse.success(orgEnterpriseProfileService.getProfile(currentUser(authentication)));
    }

    @GetMapping("/activation-code")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseActivationCodeResponseData> getActivationCode(Authentication authentication) {
        return ApiResponse.success(orgEnterpriseProfileService.getActivationCode(currentUser(authentication)));
    }

    @PostMapping("/activation-code/rotate")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> rotateActivationCode(Authentication authentication) {
        return ApiResponse.success(orgEnterpriseProfileService.rotateActivationCode(currentUser(authentication)));
    }

    @PostMapping("/activation-code/disable")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> disableActivationCode(Authentication authentication) {
        return ApiResponse.success(orgEnterpriseProfileService.disableActivationCode(currentUser(authentication)));
    }

    @GetMapping("/device-bind-logs")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseDeviceBindLogPageResponseData> getDeviceBindLogs(@RequestParam(required = false) Integer page,
                                                                                  @RequestParam(required = false) Integer size,
                                                                                  Authentication authentication) {
        return ApiResponse.success(orgEnterpriseProfileService.listDeviceBindLogs(currentUser(authentication), page, size));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
