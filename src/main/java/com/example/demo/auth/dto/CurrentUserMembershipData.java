package com.example.demo.auth.dto;

public record CurrentUserMembershipData(
        String role,
        String scopeType,
        Long enterpriseId,
        Long fleetId
) {
}
