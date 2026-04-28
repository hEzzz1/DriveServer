package com.example.demo.enterprise.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.EnterpriseReadRole;
import com.example.demo.auth.security.SuperAdminOnly;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.enterprise.dto.CreateEnterpriseRequest;
import com.example.demo.enterprise.dto.EnterpriseDetailResponseData;
import com.example.demo.enterprise.dto.EnterprisePageResponseData;
import com.example.demo.enterprise.dto.UpdateEnterpriseRequest;
import com.example.demo.enterprise.dto.UpdateEnterpriseStatusRequest;
import com.example.demo.enterprise.service.EnterpriseManagementService;
import jakarta.validation.Valid;
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

    public EnterpriseController(EnterpriseManagementService enterpriseManagementService) {
        this.enterpriseManagementService = enterpriseManagementService;
    }

    @GetMapping
    @EnterpriseReadRole
    public ApiResponse<EnterprisePageResponseData> listEnterprises(@RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Byte status,
                                                                   Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.listEnterprises(currentUser(authentication), page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @EnterpriseReadRole
    public ApiResponse<EnterpriseDetailResponseData> getEnterprise(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.getEnterprise(currentUser(authentication), id));
    }

    @PostMapping
    @SuperAdminOnly
    public ApiResponse<EnterpriseDetailResponseData> createEnterprise(@Valid @RequestBody CreateEnterpriseRequest request,
                                                                      Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.createEnterprise(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @SuperAdminOnly
    public ApiResponse<EnterpriseDetailResponseData> updateEnterprise(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdateEnterpriseRequest request,
                                                                      Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.updateEnterprise(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @SuperAdminOnly
    public ApiResponse<EnterpriseDetailResponseData> updateEnterpriseStatus(@PathVariable Long id,
                                                                            @Valid @RequestBody UpdateEnterpriseStatusRequest request,
                                                                            Authentication authentication) {
        return ApiResponse.success(enterpriseManagementService.updateStatus(currentUser(authentication), id, request.status()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
