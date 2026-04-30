package com.example.demo.auth.model;

import java.util.Locale;
import java.util.Optional;

public enum ScopeType {
    PLATFORM,
    ENTERPRISE,
    FLEET;

    public static Optional<ScopeType> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ScopeType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
