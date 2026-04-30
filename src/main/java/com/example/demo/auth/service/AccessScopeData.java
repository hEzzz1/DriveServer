package com.example.demo.auth.service;

import com.example.demo.auth.model.ScopeType;

public record AccessScopeData(
        ScopeType scopeType,
        Long enterpriseId,
        Long fleetId
) {
}
