package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignDriverFleetRequest(
        @NotNull(message = "fleetId不能为空")
        Long fleetId
) {
}
