package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeviceRequest(
        Long enterpriseId,
        Long fleetId,
        Long vehicleId,
        @NotBlank(message = "deviceCode不能为空")
        String deviceCode,
        @NotBlank(message = "deviceName不能为空")
        String deviceName,
        String activationCode,
        String remark
) {
}
