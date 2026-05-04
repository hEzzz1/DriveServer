package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetDriverPinRequest(
        @NotBlank(message = "签到码不能为空")
        String pin
) {
}
