package com.example.demo.session.dto;

import jakarta.validation.constraints.NotBlank;

public record SignInSessionRequest(
        @NotBlank(message = "driverCode不能为空")
        String driverCode,
        @NotBlank(message = "pin不能为空")
        String pin
) {
}
