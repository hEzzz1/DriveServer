package com.example.demo.session.model;

public enum SessionStatus {
    ACTIVE((byte) 1),
    CLOSED((byte) 2);

    private final byte code;

    SessionStatus(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
