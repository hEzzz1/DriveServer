package com.example.demo.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRolesRequest(
        @NotNull(message = "roles不能为空")
        List<String> roles
) {
}
