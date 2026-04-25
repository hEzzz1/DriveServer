package com.example.demo.system.dto;

public record SystemVersionResponseData(
        String applicationName,
        String version,
        String buildTime,
        String gitCommit
) {
}

