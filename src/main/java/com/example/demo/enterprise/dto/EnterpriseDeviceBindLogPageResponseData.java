package com.example.demo.enterprise.dto;

import java.util.List;

public record EnterpriseDeviceBindLogPageResponseData(
        long total,
        int page,
        int size,
        List<EnterpriseDeviceBindLogItemData> items
) {
}
