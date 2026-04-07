package com.example.demo.rule.model;

public enum RiskLevel {
    NORMAL(0),
    LOW(1),
    MID(2),
    HIGH(3);

    private final int code;

    RiskLevel(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
