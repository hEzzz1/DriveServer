package com.example.demo.system.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.system.dto.SystemAuditDetailData;
import com.example.demo.system.dto.SystemAuditExportResponseData;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.system.service.SystemAuditService;
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
@RequestMapping("/api/v1/org/audit")
public class OrgAuditController {

    private final SystemAuditService systemAuditService;

    public OrgAuditController(SystemAuditService systemAuditService) {
        this.systemAuditService = systemAuditService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.read')")
    public ApiResponse<SystemAuditPageResponseData> list(@RequestParam(required = false) String module,
                                                         @RequestParam(required = false) String actionType,
                                                         @RequestParam(required = false) String targetType,
                                                         @RequestParam(required = false) String targetId,
                                                         @RequestParam(required = false) Long actionBy,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size,
                                                         Authentication authentication) {
        return ApiResponse.success(systemAuditService.listForOrg(
                currentUser(authentication), module, actionType, targetType, targetId, actionBy, startTime, endTime, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.read')")
    public ApiResponse<SystemAuditDetailData> detail(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(systemAuditService.getDetailForOrg(currentUser(authentication), id));
    }

    @GetMapping("/export")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'audit.export')")
    public ApiResponse<SystemAuditExportResponseData> export(@RequestParam(required = false) String module,
                                                             @RequestParam(required = false) String actionType,
                                                             @RequestParam(required = false) String targetType,
                                                             @RequestParam(required = false) String targetId,
                                                             @RequestParam(required = false) Long actionBy,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                             Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        SystemAuditExportResponseData response = systemAuditService.exportForOrg(user, module, actionType, targetType, targetId, actionBy, startTime, endTime);
        systemAuditService.record(user, "AUDIT", "EXPORT_AUDITS", "AUDIT", null, "SUCCESS",
                "导出企业审计日志", java.util.Map.of("exportedCount", response.total()));
        return ApiResponse.success(response);
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
