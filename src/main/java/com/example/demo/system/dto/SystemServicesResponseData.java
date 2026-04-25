package com.example.demo.system.dto;

import java.util.List;

public record SystemServicesResponseData(
        List<SystemServiceStatusItemData> items
) {
}

