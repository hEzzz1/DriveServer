package com.example.demo.realtime.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.stats.dto.StatsOverviewResponseData;
import com.example.demo.stats.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeOverviewController {

    private final StatsService statsService;

    public RealtimeOverviewController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'overview.read')")
    public ApiResponse<StatsOverviewResponseData> overview(@RequestParam(required = false) Long fleetId,
                                                           Authentication authentication) {
        return ApiResponse.success(statsService.getRealtimeOverview(currentUser(authentication), fleetId));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
