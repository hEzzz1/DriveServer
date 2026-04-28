package com.example.demo.fleet.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFleetStatusRequest(
        @NotNull(message = "status不能为空")
        Byte status
) {
}
