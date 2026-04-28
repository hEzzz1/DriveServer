package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetDriverPinRequest(
        @NotBlank(message = "pin不能为空")
        String pin
) {
}
