package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeviceRequest(
        @NotNull(message = "vehicleId不能为空")
        Long vehicleId,
        @NotBlank(message = "deviceCode不能为空")
        String deviceCode,
        @NotBlank(message = "deviceName不能为空")
        String deviceName,
        String activationCode,
        String remark
) {
}
