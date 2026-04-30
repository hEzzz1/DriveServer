package com.example.demo.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectEdgeDeviceBindRequest(
        @NotBlank(message = "reason不能为空")
        @Size(max = 255, message = "reason长度不能超过255")
        String reason
) {
}
