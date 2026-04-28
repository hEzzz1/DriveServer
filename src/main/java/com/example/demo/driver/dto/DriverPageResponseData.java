package com.example.demo.driver.dto;

import java.util.List;

public record DriverPageResponseData(
        long total,
        int page,
        int size,
        List<DriverListItemData> items
) {
}
