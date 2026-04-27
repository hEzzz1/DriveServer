package com.example.demo.enterprise.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEnterpriseRequest(
        @NotBlank(message = "code不能为空")
        String code,
        @NotBlank(message = "name不能为空")
        String name,
        String contactName,
        String contactPhone,
        String remark
) {
}
