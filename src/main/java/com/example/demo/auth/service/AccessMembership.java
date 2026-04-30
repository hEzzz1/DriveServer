package com.example.demo.auth.service;

import com.example.demo.auth.model.ScopeType;

public record AccessMembership(
        String role,
        ScopeType scopeType,
        Long enterpriseId,
        Long fleetId
) {
}
