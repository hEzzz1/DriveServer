package com.example.demo.session.dto;

import java.util.List;

public record SessionAdminPageResponseData(
        long total,
        int page,
        int size,
        List<SessionAdminListItemData> items
) {
}
