package com.example.demo.device.dto;

import java.util.List;

public record EdgeDeviceBindRequestPageResponseData(
        long total,
        int page,
        int size,
        List<EdgeDeviceBindRequestResponseData> items
) {
}
