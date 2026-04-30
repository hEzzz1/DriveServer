package com.example.demo.device.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEdgeDeviceBindRequest(
        @NotNull(message = "enterpriseId不能为空")
        Long enterpriseId,
        @Size(max = 255, message = "remark长度不能超过255")
        String remark
) {
}
