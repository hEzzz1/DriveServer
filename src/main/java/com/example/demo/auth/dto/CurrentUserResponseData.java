package com.example.demo.auth.dto;

import java.util.List;

public class CurrentUserResponseData {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final List<String> roles;
    private final List<String> platformRoles;
    private final List<CurrentUserMembershipData> memberships;
    private final List<String> permissions;
    private final CurrentUserScopeData defaultScope;
    private final Long enterpriseId;
    private final String enterpriseName;
    private final String subjectType;
    private final boolean enabled;

    public CurrentUserResponseData(Long userId,
                                   String username,
                                   String nickname,
                                   List<String> roles,
                                   List<String> platformRoles,
                                   List<CurrentUserMembershipData> memberships,
                                   List<String> permissions,
                                   CurrentUserScopeData defaultScope,
                                   Long enterpriseId,
                                   String enterpriseName,
                                   String subjectType,
                                   boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.roles = roles;
        this.platformRoles = platformRoles;
        this.memberships = memberships;
        this.permissions = permissions;
        this.defaultScope = defaultScope;
        this.enterpriseId = enterpriseId;
        this.enterpriseName = enterpriseName;
        this.subjectType = subjectType;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPlatformRoles() {
        return platformRoles;
    }

    public List<CurrentUserMembershipData> getMemberships() {
        return memberships;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public CurrentUserScopeData getDefaultScope() {
        return defaultScope;
    }

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
