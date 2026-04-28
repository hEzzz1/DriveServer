package com.example.demo.session.dto;

import jakarta.validation.constraints.Size;

public record ForceSignOutSessionRequest(
        @Size(max = 255, message = "remark长度不能超过255")
        String remark
) {
}
