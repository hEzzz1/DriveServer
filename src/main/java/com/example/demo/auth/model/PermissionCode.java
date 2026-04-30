package com.example.demo.auth.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum PermissionCode {
    OVERVIEW_READ("overview.read"),
    ALERT_READ("alert.read"),
    ALERT_HANDLE("alert.handle"),
    STATS_READ("stats.read"),
    RULE_READ("rule.read"),
    RULE_MANAGE("rule.manage"),
    AUDIT_READ("audit.read"),
    AUDIT_EXPORT("audit.export"),
    SYSTEM_READ("system.read"),
    USER_READ("user.read"),
    USER_MANAGE("user.manage"),
    ENTERPRISE_READ("enterprise.read"),
    ENTERPRISE_MANAGE("enterprise.manage"),
    ACTIVATION_CODE_READ("activation_code.read"),
    ACTIVATION_CODE_MANAGE("activation_code.manage"),
    FLEET_READ("fleet.read"),
    FLEET_MANAGE("fleet.manage"),
    DRIVER_READ("driver.read"),
    DRIVER_MANAGE("driver.manage"),
    VEHICLE_READ("vehicle.read"),
    VEHICLE_MANAGE("vehicle.manage"),
    DEVICE_READ("device.read"),
    DEVICE_MANAGE("device.manage"),
    SESSION_READ("session.read"),
    SESSION_FORCE_SIGN_OUT("session.force_sign_out");

    private final String code;

    PermissionCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<PermissionCode> fromCode(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(permissionCode -> permissionCode.code.equals(normalized))
                .findFirst();
    }

    public static List<String> allCodes() {
        return Arrays.stream(values()).map(PermissionCode::code).toList();
    }

    public static List<String> sortCodes(Collection<String> codes) {
        if (codes == null) {
            return List.of();
        }
        return codes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(Comparator
                        .comparingInt((String code) -> fromCode(code).map(Enum::ordinal).orElse(Integer.MAX_VALUE))
                        .thenComparing(String::compareTo))
                .toList();
    }
}
