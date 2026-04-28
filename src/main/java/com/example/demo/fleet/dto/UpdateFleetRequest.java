package com.example.demo.fleet.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFleetRequest(
        @NotBlank(message = "name不能为空")
        String name,
        String remark
) {
}
