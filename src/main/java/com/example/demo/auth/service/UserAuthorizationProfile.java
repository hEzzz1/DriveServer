package com.example.demo.auth.service;

import java.util.List;

public record UserAuthorizationProfile(
        List<String> roles,
        List<String> platformRoles,
        List<AccessMembership> memberships,
        List<String> permissions,
        AccessScopeData defaultScope,
        BusinessDataScope dataScope
) {

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasPlatformRole(String roleCode) {
        return platformRoles.contains(roleCode);
    }
}
