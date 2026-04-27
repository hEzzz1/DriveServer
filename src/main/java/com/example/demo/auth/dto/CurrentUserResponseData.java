package com.example.demo.auth.dto;

import java.util.List;

public class CurrentUserResponseData {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final List<String> roles;
    private final Long enterpriseId;
    private final String enterpriseName;
    private final String subjectType;
    private final boolean enabled;

    public CurrentUserResponseData(Long userId,
                                   String username,
                                   String nickname,
                                   List<String> roles,
                                   Long enterpriseId,
                                   String enterpriseName,
                                   String subjectType,
                                   boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.roles = roles;
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
