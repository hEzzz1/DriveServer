package com.example.demo.auth.controller;

import com.example.demo.auth.dto.CurrentUserResponseData;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponseData;
import com.example.demo.auth.security.AdminOnly;
import com.example.demo.auth.security.AnyUserRole;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.AuthService;
import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    @AnyUserRole
    public ApiResponse<CurrentUserResponseData> currentUser(Authentication authentication) {
        AuthenticatedUser current = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.success(new CurrentUserResponseData(current.getUserId(), current.getUsername(), current.getRoles()));
    }

    @GetMapping("/admin/ping")
    @AdminOnly
    public ApiResponse<Map<String, String>> adminPing() {
        return ApiResponse.success(Map.of("message", "admin-ok"));
    }
}
