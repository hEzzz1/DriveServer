package com.example.demo.alert.controller;

import com.example.demo.alert.dto.AlertActionLogsResponseData;
import com.example.demo.alert.dto.AlertActionRequest;
import com.example.demo.alert.dto.AlertDetailResponseData;
import com.example.demo.alert.dto.AlertOperationResponseData;
import com.example.demo.alert.dto.AlertPageResponseData;
import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.service.AlertService;
import com.example.demo.alert.service.AlertEvidenceService;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping({"/api/v1/alerts", "/api/v1/org/alerts"})
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.handle')")
    public ApiResponse<AlertOperationResponseData> createAlert(@Valid @RequestBody CreateAlertRequest request,
                                                               Authentication authentication) {
        return ApiResponse.success(alertService.createAlert(request, currentUser(authentication)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.read')")
    public ApiResponse<AlertPageResponseData> listAlerts(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size,
                                                         @RequestParam(required = false) Long fleetId,
                                                         @RequestParam(required = false) Long vehicleId,
                                                         @RequestParam(required = false) Long driverId,
                                                         @RequestParam(required = false) Integer riskLevel,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                         Authentication authentication) {
        return ApiResponse.success(alertService.listAlerts(
                page, size, currentUser(authentication), fleetId, vehicleId, driverId, riskLevel, status, startTime, endTime));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.read')")
    public ApiResponse<AlertDetailResponseData> detail(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(alertService.getAlertDetail(id, currentUser(authentication)));
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.read')")
    public ResponseEntity<Resource> evidence(@PathVariable Long id, Authentication authentication) {
        AlertEvidenceService.EvidenceResource evidence = alertService.getAlertEvidence(id, currentUser(authentication));
        return ResponseEntity.ok()
                .contentType(evidence.mediaType())
                .contentLength(evidence.contentLength())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(evidence.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(evidence.resource());
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.handle')")
    public ApiResponse<AlertOperationResponseData> confirmAlert(@PathVariable Long id,
                                                                @Valid @RequestBody AlertActionRequest request,
                                                                Authentication authentication) {
        return ApiResponse.success(alertService.confirmAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/false-positive")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.handle')")
    public ApiResponse<AlertOperationResponseData> falsePositive(@PathVariable Long id,
                                                                 @Valid @RequestBody AlertActionRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.success(alertService.markFalsePositive(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.handle')")
    public ApiResponse<AlertOperationResponseData> close(@PathVariable Long id,
                                                         @Valid @RequestBody AlertActionRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(alertService.closeAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @GetMapping("/{id}/action-logs")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'alert.read')")
    public ApiResponse<AlertActionLogsResponseData> actionLogs(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(alertService.listActionLogs(id, currentUser(authentication)));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
