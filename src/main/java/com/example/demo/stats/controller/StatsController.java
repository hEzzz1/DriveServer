package com.example.demo.stats.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.stats.dto.RankingResponseData;
import com.example.demo.stats.dto.TrendResponseData;
import com.example.demo.stats.model.RankingDimension;
import com.example.demo.stats.model.RankingSortBy;
import com.example.demo.stats.model.TrendGroupBy;
import com.example.demo.stats.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping({"/api/v1/stats", "/api/v1/org/stats"})
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/trend")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'stats.read')")
    public ApiResponse<TrendResponseData> trend(@RequestParam(required = false) Long fleetId,
                                                @RequestParam(required = false) Integer riskLevel,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                @RequestParam(defaultValue = "HOUR") TrendGroupBy groupBy,
                                                Authentication authentication) {
        return ApiResponse.success(statsService.getTrend(currentUser(authentication), fleetId, riskLevel, status, startTime, endTime, groupBy));
    }

    @GetMapping("/ranking")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'stats.read')")
    public ApiResponse<RankingResponseData> ranking(@RequestParam(required = false) Long fleetId,
                                                    @RequestParam(required = false) Integer riskLevel,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
                                                    @RequestParam(defaultValue = "DRIVER_ID") RankingDimension dimension,
                                                    @RequestParam(defaultValue = "ALERT_COUNT") RankingSortBy sortBy,
                                                    @RequestParam(required = false) Integer limit,
                                                    Authentication authentication) {
        return ApiResponse.success(statsService.getRanking(
                currentUser(authentication), fleetId, riskLevel, status, startTime, endTime, dimension, sortBy, limit));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
