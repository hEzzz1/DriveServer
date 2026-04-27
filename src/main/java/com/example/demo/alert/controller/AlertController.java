package com.example.demo.alert.controller;

import com.example.demo.alert.dto.AlertActionLogsResponseData;
import com.example.demo.alert.dto.AlertActionRequest;
import com.example.demo.alert.dto.AlertDetailResponseData;
import com.example.demo.alert.dto.AlertOperationResponseData;
import com.example.demo.alert.dto.AlertPageResponseData;
import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.service.AlertService;
import com.example.demo.auth.security.AnyReadRole;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.OperatorOrSuperAdmin;
import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @OperatorOrSuperAdmin
    public ApiResponse<AlertOperationResponseData> createAlert(@Valid @RequestBody CreateAlertRequest request,
                                                               Authentication authentication) {
        return ApiResponse.success(alertService.createAlert(request, currentUser(authentication)));
    }

    @GetMapping
    @AnyReadRole
    public ApiResponse<AlertPageResponseData> listAlerts(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size,
                                                         @RequestParam(required = false) Long fleetId,
                                                         @RequestParam(required = false) Long vehicleId,
                                                         @RequestParam(required = false) Long driverId,
                                                         @RequestParam(required = false) Integer riskLevel,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return ApiResponse.success(alertService.listAlerts(
                page, size, fleetId, vehicleId, driverId, riskLevel, status, startTime, endTime));
    }

    @GetMapping("/{id}")
    @AnyReadRole
    public ApiResponse<AlertDetailResponseData> detail(@PathVariable Long id) {
        return ApiResponse.success(alertService.getAlertDetail(id));
    }

    @PostMapping("/{id}/confirm")
    @OperatorOrSuperAdmin
    public ApiResponse<AlertOperationResponseData> confirmAlert(@PathVariable Long id,
                                                                @Valid @RequestBody AlertActionRequest request,
                                                                Authentication authentication) {
        return ApiResponse.success(alertService.confirmAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/false-positive")
    @OperatorOrSuperAdmin
    public ApiResponse<AlertOperationResponseData> falsePositive(@PathVariable Long id,
                                                                 @Valid @RequestBody AlertActionRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.success(alertService.markFalsePositive(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/close")
    @OperatorOrSuperAdmin
    public ApiResponse<AlertOperationResponseData> close(@PathVariable Long id,
                                                         @Valid @RequestBody AlertActionRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(alertService.closeAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @GetMapping("/{id}/action-logs")
    @AnyReadRole
    public ApiResponse<AlertActionLogsResponseData> actionLogs(@PathVariable Long id) {
        return ApiResponse.success(alertService.listActionLogs(id));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
