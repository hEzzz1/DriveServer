package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(
        @NotNull(message = "status不能为空")
        Byte status
) {
}
