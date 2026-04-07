package com.example.demo.alert.controller;

import com.example.demo.alert.dto.AlertActionLogsResponseData;
import com.example.demo.alert.dto.AlertActionRequest;
import com.example.demo.alert.dto.AlertOperationResponseData;
import com.example.demo.alert.dto.CreateAlertRequest;
import com.example.demo.alert.service.AlertService;
import com.example.demo.auth.security.AdminOrOperator;
import com.example.demo.auth.security.AnyUserRole;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @AdminOrOperator
    public ApiResponse<AlertOperationResponseData> createAlert(@Valid @RequestBody CreateAlertRequest request,
                                                               Authentication authentication) {
        return ApiResponse.success(alertService.createAlert(request, currentUser(authentication)));
    }

    @PostMapping("/{id}/confirm")
    @AdminOrOperator
    public ApiResponse<AlertOperationResponseData> confirmAlert(@PathVariable Long id,
                                                                @Valid @RequestBody AlertActionRequest request,
                                                                Authentication authentication) {
        return ApiResponse.success(alertService.confirmAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/false-positive")
    @AdminOrOperator
    public ApiResponse<AlertOperationResponseData> falsePositive(@PathVariable Long id,
                                                                 @Valid @RequestBody AlertActionRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.success(alertService.markFalsePositive(id, request.getRemark(), currentUser(authentication)));
    }

    @PostMapping("/{id}/close")
    @AdminOrOperator
    public ApiResponse<AlertOperationResponseData> close(@PathVariable Long id,
                                                         @Valid @RequestBody AlertActionRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(alertService.closeAlert(id, request.getRemark(), currentUser(authentication)));
    }

    @GetMapping("/{id}/action-logs")
    @AnyUserRole
    public ApiResponse<AlertActionLogsResponseData> actionLogs(@PathVariable Long id) {
        return ApiResponse.success(alertService.listActionLogs(id));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
