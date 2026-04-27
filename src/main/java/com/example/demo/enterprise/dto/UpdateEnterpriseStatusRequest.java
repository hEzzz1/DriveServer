package com.example.demo.enterprise.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateEnterpriseStatusRequest(
        @NotNull(message = "enabled不能为空")
        Boolean enabled
) {
}
