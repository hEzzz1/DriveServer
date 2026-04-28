package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceActivateRequest(
        @NotBlank(message = "deviceCode不能为空")
        String deviceCode,
        @NotBlank(message = "activationCode不能为空")
        String activationCode
) {
}
