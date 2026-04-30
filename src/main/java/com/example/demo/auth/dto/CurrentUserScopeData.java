package com.example.demo.auth.dto;

public record CurrentUserScopeData(
        String scopeType,
        Long enterpriseId,
        Long fleetId
) {
}
