package com.example.demo.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "enabled不能为空")
        Boolean enabled
) {
}
