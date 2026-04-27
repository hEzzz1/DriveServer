package com.example.demo.user.dto;

import java.util.List;

public record UserPageResponseData(
        long total,
        int page,
        int size,
        List<UserListItemData> items
) {
}
