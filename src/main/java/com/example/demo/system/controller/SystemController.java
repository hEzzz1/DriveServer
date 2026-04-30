package com.example.demo.system.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.system.dto.SystemAuditDetailData;
import com.example.demo.system.dto.SystemAuditExportResponseData;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.system.dto.SystemHealthResponseData;
import com.example.demo.system.dto.SystemMonitoringResponseData;
import com.example.demo.system.dto.SystemServicesResponseData;
import com.example.demo.system.dto.SystemSummaryResponseData;
import com.example.demo.system.dto.SystemVersionResponseData;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.system.service.SystemManagementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
public class SystemController {

    private final SystemManagementService systemManagementService;
    private final SystemAuditService systemAuditService;

    public SystemController(SystemManagementService systemManagementService, SystemAuditService systemAuditService) {
        this.systemManagementService = systemManagementService;
        this.systemAuditService = systemAuditService;
    }

    @GetMapping("/system/health")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'system.read')")
    public ApiResponse<SystemHealthResponseData> health() {
        return ApiResponse.success(systemManagementService.getHealth());
    }

    @GetMapping("/system/services")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'system.read')")
    public ApiResponse<SystemServicesResponseData> services() {
        return ApiResponse.success(systemManagementService.getServices());
    }

    @GetMapping("/system/version")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'system.read')")
    public ApiResponse<SystemVersionResponseData> version() {
        return ApiResponse.success(systemManagementService.getVersion());
    }

    @GetMapping("/system/monitoring")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'system.read')")
    public ApiResponse<SystemMonitoringResponseData> monitoring() {
        return ApiResponse.success(systemManagementService.getMonitoring());
    }

    @GetMapping("/system/summary")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'system.read')")
    public ApiResponse<SystemSummaryResponseData> summary() {
        return ApiResponse.success(systemManagementService.getSummary());
    }

    @GetMapping("/audits")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.read')")
    public ApiResponse<SystemAuditPageResponseData> audits(@RequestParam(required = false) String module,
                                                           @RequestParam(required = false) String actionType,
                                                           @RequestParam(required = false) String targetType,
                                                           @RequestParam(required = false) String targetId,
                                                           @RequestParam(required = false) Long actionBy,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        return ApiResponse.success(systemAuditService.list(module, actionType, targetType, targetId, actionBy, startTime, endTime, page, size));
    }

    @GetMapping("/audits/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.read')")
    public ApiResponse<SystemAuditDetailData> auditDetail(@PathVariable Long id) {
        return ApiResponse.success(systemAuditService.getDetail(id));
    }

    @GetMapping("/audits/export")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.export')")
    public ApiResponse<SystemAuditExportResponseData> exportAudits(@RequestParam(required = false) String module,
                                                                   @RequestParam(required = false) String actionType,
                                                                   @RequestParam(required = false) String targetType,
                                                                   @RequestParam(required = false) String targetId,
                                                                   @RequestParam(required = false) Long actionBy,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                                   Authentication authentication) {
        SystemAuditExportResponseData response = systemAuditService.export(module, actionType, targetType, targetId, actionBy, startTime, endTime);
        systemAuditService.record(currentUser(authentication), "SYSTEM", "EXPORT_AUDITS", "AUDIT", null, "SUCCESS",
                "导出审计日志", java.util.Map.of("exportedCount", response.total()));
        return ApiResponse.success(response);
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
