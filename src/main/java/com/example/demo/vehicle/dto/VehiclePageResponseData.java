package com.example.demo.vehicle.dto;

import java.util.List;

public record VehiclePageResponseData(
        long total,
        int page,
        int size,
        List<VehicleListItemData> items
) {
}
