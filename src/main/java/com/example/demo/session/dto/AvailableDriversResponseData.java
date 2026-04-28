package com.example.demo.session.dto;

import java.util.List;

public record AvailableDriversResponseData(
        Long deviceId,
        Long fleetId,
        List<AvailableDriverItemData> items
) {
}
