package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetUserPasswordRequest(
        @NotBlank(message = "newPassword不能为空")
        String newPassword
) {
}
