package com.example.demo.enterprise.dto;

import java.util.List;

public record EnterprisePageResponseData(
        long total,
        int page,
        int size,
        List<EnterpriseListItemData> items
) {
}
