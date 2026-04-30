package com.example.demo.device.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.EnterpriseAdminOrSuperAdmin;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.dto.EdgeDeviceBindRequestPageResponseData;
import com.example.demo.device.dto.EdgeDeviceBindRequestResponseData;
import com.example.demo.device.dto.ReviewEdgeDeviceBindRequest;
import com.example.demo.device.service.EdgeDeviceBindRequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/edge/bind-requests")
public class AdminEdgeBindRequestController {

    private final EdgeDeviceBindRequestService edgeDeviceBindRequestService;

    public AdminEdgeBindRequestController(EdgeDeviceBindRequestService edgeDeviceBindRequestService) {
        this.edgeDeviceBindRequestService = edgeDeviceBindRequestService;
    }

    @GetMapping
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<EdgeDeviceBindRequestPageResponseData> list(@RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size,
                                                                   @RequestParam(required = false) Long enterpriseId,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) String deviceCode,
                                                                   Authentication authentication) {
        return ApiResponse.success(edgeDeviceBindRequestService.list(currentUser(authentication), page, size, enterpriseId, status, deviceCode));
    }

    @GetMapping("/{id}")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<EdgeDeviceBindRequestResponseData> get(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(edgeDeviceBindRequestService.get(currentUser(authentication), id));
    }

    @PostMapping("/{id}/approve")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<EdgeDeviceBindRequestResponseData> approve(@PathVariable Long id,
                                                                  @Valid @RequestBody(required = false) ReviewEdgeDeviceBindRequest request,
                                                                  Authentication authentication) {
        return ApiResponse.success(edgeDeviceBindRequestService.approve(currentUser(authentication), id, request));
    }

    @PostMapping("/{id}/reject")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<EdgeDeviceBindRequestResponseData> reject(@PathVariable Long id,
                                                                 @Valid @RequestBody(required = false) ReviewEdgeDeviceBindRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.success(edgeDeviceBindRequestService.reject(currentUser(authentication), id, request));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
