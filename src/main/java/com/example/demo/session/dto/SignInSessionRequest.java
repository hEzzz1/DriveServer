package com.example.demo.session.dto;

import jakarta.validation.constraints.NotBlank;

public record SignInSessionRequest(
        @NotBlank(message = "driverCode不能为空")
        String driverCode,
        @NotBlank(message = "签到码不能为空")
        String pin
) {
}
