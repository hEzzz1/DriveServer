package com.example.demo.vehicle.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(
        @NotNull(message = "status不能为空")
        Byte status
) {
}
