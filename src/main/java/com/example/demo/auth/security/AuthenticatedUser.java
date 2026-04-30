package com.example.demo.auth.security;

import com.example.demo.auth.model.RoleTemplateCode;
import com.example.demo.auth.model.SubjectType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser {

    private final Long userId;
    private final String username;
    private final SubjectType subjectType;
    private final List<String> roles;

    public AuthenticatedUser(Long userId, String username, List<String> roles) {
        this(userId, username, SubjectType.USER, roles);
    }

    public AuthenticatedUser(Long userId, String username, SubjectType subjectType, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.subjectType = subjectType == null ? SubjectType.USER : subjectType;
        this.roles = RoleTemplateCode.normalizeAll(roles);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
