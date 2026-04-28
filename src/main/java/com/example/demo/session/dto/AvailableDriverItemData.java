package com.example.demo.session.dto;

public record AvailableDriverItemData(
        Long id,
        String driverCode,
        String name,
        Long fleetId
) {
}
