package com.example.demo.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFleetRequest(
        @NotNull(message = "enterpriseId不能为空")
        Long enterpriseId,
        @NotBlank(message = "name不能为空")
        String name,
        String remark
) {
}
