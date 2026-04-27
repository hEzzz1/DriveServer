package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "username不能为空")
        String username,
        String nickname,
        Long enterpriseId
) {
}
