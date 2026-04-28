package com.example.demo.device.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDeviceStatusRequest(
        @NotNull(message = "status不能为空")
        Byte status
) {
}
