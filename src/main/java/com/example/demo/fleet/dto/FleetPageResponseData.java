package com.example.demo.fleet.dto;

import java.util.List;

public record FleetPageResponseData(
        long total,
        int page,
        int size,
        List<FleetListItemData> items
) {
}
