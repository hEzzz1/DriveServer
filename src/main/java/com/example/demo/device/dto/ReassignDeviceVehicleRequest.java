package com.example.demo.device.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignDeviceVehicleRequest(
        @NotNull(message = "vehicleId不能为空")
        Long vehicleId
) {
}
