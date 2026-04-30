package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimEdgeDeviceRequest(
        @NotBlank(message = "deviceCode不能为空")
        @Size(max = 64, message = "deviceCode长度不能超过64")
        String deviceCode,
        @Size(max = 128, message = "deviceName长度不能超过128")
        String deviceName,
        @NotBlank(message = "enterpriseActivationCode不能为空")
        @Size(max = 64, message = "enterpriseActivationCode长度不能超过64")
        String enterpriseActivationCode
) {
}
