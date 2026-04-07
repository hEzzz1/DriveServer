package com.example.demo.alert.model;

import java.util.Arrays;

public enum AlertStatus {
    NEW((byte) 0),
    CONFIRMED((byte) 1),
    FALSE_POSITIVE((byte) 2),
    CLOSED((byte) 3);

    private final byte code;

    AlertStatus(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static AlertStatus fromCode(Byte code) {
        if (code == null) {
            throw new IllegalArgumentException("status code must not be null");
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported status code: " + code));
    }
}
