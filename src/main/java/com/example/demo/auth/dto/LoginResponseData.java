package com.example.demo.auth.dto;

import java.time.Instant;
import java.util.List;

public class LoginResponseData {
    private String token;
    private Instant expireAt;
    private List<String> roles;

    public LoginResponseData(String token, Instant expireAt, List<String> roles) {
        this.token = token;
        this.expireAt = expireAt;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public List<String> getRoles() {
        return roles;
    }
}
