package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "username不能为空")
        String username,
        @NotBlank(message = "password不能为空")
        String password,
        String nickname,
        Long enterpriseId,
        Boolean enabled,
        List<String> roles
) {
}
