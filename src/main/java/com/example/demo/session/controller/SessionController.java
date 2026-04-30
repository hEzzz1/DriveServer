package com.example.demo.session.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.session.dto.ForceSignOutSessionRequest;
import com.example.demo.session.dto.SessionAdminDetailResponseData;
import com.example.demo.session.dto.SessionAdminPageResponseData;
import com.example.demo.session.service.DrivingSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final DrivingSessionService drivingSessionService;

    public SessionController(DrivingSessionService drivingSessionService) {
        this.drivingSessionService = drivingSessionService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'session.read')")
    public ApiResponse<SessionAdminPageResponseData> listSessions(@RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size,
                                                                  @RequestParam(required = false) Long enterpriseId,
                                                                  @RequestParam(required = false) Long fleetId,
                                                                  @RequestParam(required = false) Byte status,
                                                                  @RequestParam(required = false) String keyword,
                                                                  Authentication authentication) {
        return ApiResponse.success(drivingSessionService.listSessions(currentUser(authentication), page, size, enterpriseId, fleetId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'session.read')")
    public ApiResponse<SessionAdminDetailResponseData> getSession(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(drivingSessionService.getSession(currentUser(authentication), id));
    }

    @PutMapping("/{id}/force-sign-out")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'session.force_sign_out')")
    public ApiResponse<SessionAdminDetailResponseData> forceSignOut(@PathVariable Long id,
                                                                    @Valid @RequestBody(required = false) ForceSignOutSessionRequest request,
                                                                    Authentication authentication) {
        return ApiResponse.success(drivingSessionService.forceSignOut(currentUser(authentication), id, request == null ? null : request.remark()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
