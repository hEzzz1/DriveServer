package com.example.demo.system.dto;

import java.util.Map;

public record SystemHealthResponseData(
        String status,
        Map<String, Object> details
) {
}

