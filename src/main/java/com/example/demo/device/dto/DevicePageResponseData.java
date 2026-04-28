package com.example.demo.device.dto;

import java.util.List;

public record DevicePageResponseData(
        long total,
        int page,
        int size,
        List<DeviceListItemData> items
) {
}
