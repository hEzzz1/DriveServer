package com.example.demo.auth.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum RoleCode {
    SUPER_ADMIN,
    ENTERPRISE_ADMIN,
    SYS_ADMIN,
    RISK_ADMIN,
    OPERATOR,
    ANALYST,
    VIEWER;

    public static Optional<RoleCode> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(RoleCode.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static List<String> normalizeAll(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(RoleCode::from)
                .flatMap(Optional::stream)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(RoleCode::name)
                .toList();
    }

    public static List<String> names() {
        return Arrays.stream(values()).map(RoleCode::name).toList();
    }
}
