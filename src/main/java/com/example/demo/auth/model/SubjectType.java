package com.example.demo.auth.model;

import java.util.Locale;

public enum SubjectType {
    USER,
    SYSTEM;

    public static SubjectType from(String value) {
        if (value == null) {
            return USER;
        }
        try {
            return SubjectType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }
}
