package com.example.demo.enterprise.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.enterprise.dto.EnterpriseActivationCodeResponseData;
import com.example.demo.enterprise.dto.CreateEnterpriseRequest;
import com.example.demo.enterprise.dto.EnterpriseDetailResponseData;
import com.example.demo.enterprise.dto.EnterpriseDeviceBindLogPageResponseData;
import com.example.demo.enterprise.dto.EnterprisePageResponseData;
import com.example.demo.enterprise.dto.UpdateEnterpriseRequest;
import com.example.demo.enterprise.dto.UpdateEnterpriseStatusRequest;
import com.example.demo.device.service.EnterpriseDeviceBindLogService;
import com.example.demo.enterprise.service.EnterpriseActivationCodeService;
import com.example.demo.enterprise.service.EnterpriseManagementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enterprises")
public class EnterpriseController {

    private final EnterpriseManagementService enterpriseManagementService;
    private final EnterpriseActivationCodeService enterpriseActivationCodeService;
    private final EnterpriseDeviceBindLogService enterpriseDeviceBindLogService;

    public EnterpriseController(EnterpriseManagementService enterpriseManagementService,
                                EnterpriseActivationCodeService enterpriseActivationCodeService,
                                EnterpriseDeviceBindLogService enterpriseDeviceBindLogService) {
        this.enterpriseManagementService = enterpriseManagementService;
        this.enterpriseActivationCodeService = enterpriseActivationCodeService;
        this.enterpriseDeviceBindLogService = enterpriseDeviceBindLogService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.read')")
    public ApiResponse<EnterprisePageResponseData> listEnterprises(@RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Byte status,
                                                                   Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.listEnterprises(currentUser(authentication), page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.read')")
    public ApiResponse<EnterpriseDetailResponseData> getEnterprise(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.getEnterprise(currentUser(authentication), id));
    }

    @GetMapping("/{id}/activation-code")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseActivationCodeResponseData> getActivationCode(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.getActivationCode(currentUser(authentication), id));
    }

    @PostMapping("/{id}/activation-code/rotate")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> rotateActivationCode(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.rotateActivationCode(currentUser(authentication), id));
    }

    @PostMapping("/{id}/activation-code/disable")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.manage')")
    public ApiResponse<EnterpriseActivationCodeResponseData> disableActivationCode(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(enterpriseActivationCodeService.disableActivationCode(currentUser(authentication), id));
    }

    @GetMapping("/{id}/device-bind-logs")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'activation_code.read')")
    public ApiResponse<EnterpriseDeviceBindLogPageResponseData> getDeviceBindLogs(@PathVariable Long id,
                                                                                  @RequestParam(required = false) Integer page,
                                                                                  @RequestParam(required = false) Integer size,
                                                                                  Authentication authentication) {
        return ApiResponse.success(enterpriseDeviceBindLogService.list(currentUser(authentication), id, page, size));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.manage')")
    public ApiResponse<EnterpriseDetailResponseData> createEnterprise(@Valid @RequestBody CreateEnterpriseRequest request,
                                                                      Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.createEnterprise(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.manage')")
    public ApiResponse<EnterpriseDetailResponseData> updateEnterprise(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdateEnterpriseRequest request,
                                                                      Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.updateEnterprise(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'enterprise.manage')")
    public ApiResponse<EnterpriseDetailResponseData> updateEnterpriseStatus(@PathVariable Long id,
                                                                            @Valid @RequestBody UpdateEnterpriseStatusRequest request,
                                                                            Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.updateStatus(currentUser(authentication), id, request.status()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
