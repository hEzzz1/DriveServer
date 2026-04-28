package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDeviceRequest(
        @NotBlank(message = "deviceName不能为空")
        String deviceName,
        String activationCode,
        String remark
) {
}
