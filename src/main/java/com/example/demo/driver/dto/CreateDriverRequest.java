package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDriverRequest(
        @NotNull(message = "enterpriseId不能为空")
        Long enterpriseId,
        Long fleetId,
        String driverCode,
        @NotBlank(message = "name不能为空")
        String name,
        String phone,
        String licenseNo,
        String remark
) {
}
