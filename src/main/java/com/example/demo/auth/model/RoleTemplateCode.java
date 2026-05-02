package com.example.demo.auth.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum RoleTemplateCode {
    PLATFORM_SUPER_ADMIN(true, "平台超级管理员"),
    PLATFORM_SYS_ADMIN(true, "平台系统管理员"),
    PLATFORM_RISK_ADMIN(true, "平台风控管理员"),
    ORG_ADMIN(false, "组织管理员"),
    ORG_OPERATOR(false, "组织运营处理人员"),
    ORG_ANALYST(false, "组织分析查看人员"),
    ORG_VIEWER(false, "组织只读观察人员");

    private final boolean platformRole;
    private final String displayName;

    RoleTemplateCode(boolean platformRole, String displayName) {
        this.platformRole = platformRole;
        this.displayName = displayName;
    }

    public boolean isPlatformRole() {
        return platformRole;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supportsScope(ScopeType scopeType) {
        if (scopeType == null) {
            return false;
        }
        return platformRole ? scopeType == ScopeType.PLATFORM : scopeType != ScopeType.PLATFORM;
    }

    public Set<String> defaultPermissions() {
        return switch (this) {
            case PLATFORM_SUPER_ADMIN -> new LinkedHashSet<>(PermissionCode.allCodes());
            case PLATFORM_SYS_ADMIN -> Set.of(
                    PermissionCode.AUDIT_READ.code(),
                    PermissionCode.AUDIT_EXPORT.code(),
                    PermissionCode.SYSTEM_READ.code());
            case PLATFORM_RISK_ADMIN -> Set.of(
                    PermissionCode.OVERVIEW_READ.code(),
                    PermissionCode.ALERT_READ.code(),
                    PermissionCode.STATS_READ.code(),
                    PermissionCode.RULE_READ.code(),
                    PermissionCode.RULE_MANAGE.code());
            case ORG_ADMIN -> Set.of(
                    PermissionCode.AUDIT_READ.code(),
                    PermissionCode.AUDIT_EXPORT.code(),
                    PermissionCode.OVERVIEW_READ.code(),
                    PermissionCode.ALERT_READ.code(),
                    PermissionCode.STATS_READ.code(),
                    PermissionCode.USER_READ.code(),
                    PermissionCode.USER_MANAGE.code(),
                    PermissionCode.ENTERPRISE_READ.code(),
                    PermissionCode.ACTIVATION_CODE_READ.code(),
                    PermissionCode.ACTIVATION_CODE_MANAGE.code(),
                    PermissionCode.FLEET_READ.code(),
                    PermissionCode.FLEET_MANAGE.code(),
                    PermissionCode.DRIVER_READ.code(),
                    PermissionCode.DRIVER_MANAGE.code(),
                    PermissionCode.VEHICLE_READ.code(),
                    PermissionCode.VEHICLE_MANAGE.code(),
                    PermissionCode.DEVICE_READ.code(),
                    PermissionCode.DEVICE_MANAGE.code(),
                    PermissionCode.SESSION_READ.code(),
                    PermissionCode.SESSION_FORCE_SIGN_OUT.code());
            case ORG_OPERATOR -> Set.of(
                    PermissionCode.OVERVIEW_READ.code(),
                    PermissionCode.ALERT_READ.code(),
                    PermissionCode.ALERT_HANDLE.code(),
                    PermissionCode.STATS_READ.code(),
                    PermissionCode.FLEET_READ.code(),
                    PermissionCode.DRIVER_READ.code(),
                    PermissionCode.VEHICLE_READ.code(),
                    PermissionCode.DEVICE_READ.code(),
                    PermissionCode.SESSION_READ.code());
            case ORG_ANALYST -> Set.of(
                    PermissionCode.OVERVIEW_READ.code(),
                    PermissionCode.ALERT_READ.code(),
                    PermissionCode.STATS_READ.code(),
                    PermissionCode.FLEET_READ.code(),
                    PermissionCode.DRIVER_READ.code(),
                    PermissionCode.VEHICLE_READ.code(),
                    PermissionCode.DEVICE_READ.code(),
                    PermissionCode.SESSION_READ.code());
            case ORG_VIEWER -> Set.of(
                    PermissionCode.OVERVIEW_READ.code(),
                    PermissionCode.ALERT_READ.code());
        };
    }

    public static Optional<RoleTemplateCode> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(RoleTemplateCode.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static List<String> normalizeAll(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(RoleTemplateCode::from)
                .flatMap(Optional::stream)
                .distinct()
                .map(RoleTemplateCode::name)
                .toList();
    }

    public static List<String> names() {
        return Arrays.stream(values()).map(RoleTemplateCode::name).toList();
    }
}
