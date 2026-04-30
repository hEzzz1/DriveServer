package com.example.demo.auth.service;

import com.example.demo.auth.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionAuthorizationService")
public class PermissionAuthorizationService {

    private final UserAuthorizationService userAuthorizationService;

    public PermissionAuthorizationService(UserAuthorizationService userAuthorizationService) {
        this.userAuthorizationService = userAuthorizationService;
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return userAuthorizationService.loadProfile(user).hasPermission(permission);
    }
}
