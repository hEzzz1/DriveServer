package com.example.demo.auth.dto;

import java.util.List;

public class CurrentUserResponseData {

    private Long userId;
    private String username;
    private List<String> roles;

    public CurrentUserResponseData(Long userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
