package com.example.demo.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(
        @NotNull(message = "enterpriseId不能为空")
        Long enterpriseId,
        @NotNull(message = "fleetId不能为空")
        Long fleetId,
        @NotBlank(message = "plateNumber不能为空")
        String plateNumber,
        String vin,
        String remark
) {
}
