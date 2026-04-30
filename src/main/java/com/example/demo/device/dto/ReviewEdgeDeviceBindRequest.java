package com.example.demo.device.dto;

import jakarta.validation.constraints.Size;

public record ReviewEdgeDeviceBindRequest(
        @Size(max = 255, message = "remark长度不能超过255")
        String remark
) {
}
