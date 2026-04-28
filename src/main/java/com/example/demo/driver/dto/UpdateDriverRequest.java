package com.example.demo.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDriverRequest(
        String driverCode,
        @NotBlank(message = "name不能为空")
        String name,
        String phone,
        String licenseNo,
        String remark
) {
}
