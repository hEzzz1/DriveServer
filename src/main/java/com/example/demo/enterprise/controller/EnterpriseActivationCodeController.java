package com.example.demo.enterprise.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.service.EnterpriseDeviceBindLogService;
import com.example.demo.enterprise.dto.EnterpriseActivationCodeResponseData;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogPageResponseData;
import com.example.demo.enterprise.service.EnterpriseActivationCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprises/{enterpriseId}")
public class EnterpriseActivationCodeController {

    private final EnterpriseActivationCodeService enterpriseActivationCodeService;
    private final EnterpriseDeviceBindLogService enterpriseDeviceBindLogService;

    public EnterpriseActivationCodeController(EnterpriseActivationCodeService enterpriseActivationCodeService,
                                             EnterpriseDeviceBindLogService enterpriseDeviceBindLogService) {
        this.enterpriseActivationCodeService = enterpriseActivationCodeService;
        this.enterpriseDeviceBindLogService = enterpriseDeviceBindLogService;
    }

    @GetMapping("/activation-code")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseActivationCodeResponseData> getActivationCode(@PathVariable Long enterpriseId,
                                                                               Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.getActivationCode(currentUser(authentication), enterpriseId));
    }

    @PostMapping("/activation-code/rotate")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> rotateActivationCode(@PathVariable Long enterpriseId,
                                                                                  Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.rotateActivationCode(currentUser(authentication), enterpriseId));
    }

    @PostMapping("/activation-code/disable")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> disableActivationCode(@PathVariable Long enterpriseId,
                                                                                   Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.disableActivationCode(currentUser(authentication), enterpriseId));
    }

    @GetMapping("/device-bind-logs")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseDeviceBindLogPageResponseData> getDeviceBindLogs(@PathVariable Long enterpriseId,
                                                                                  @RequestParam(required = false) Integer page,
                                                                                  @RequestParam(required = false) Integer size,
                                                                                  Authentication authentication) {
        return ApiResponse.success(enterpriseDeviceBindLogService.list(currentUser(authentication), enterpriseId, page, size));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
