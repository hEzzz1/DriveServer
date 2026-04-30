package com.example.demo.common.api;

import org.springframework.http.HttpStatus;

public enum ApiCode {
    SUCCESS(0, "ok", HttpStatus.OK),
    INVALID_PARAM(40001, "请求参数不合法", HttpStatus.BAD_REQUEST),
    IDEMPOTENT_CONFLICT(40002, "幂等冲突（重复事件）", HttpStatus.CONFLICT),
    DEVICE_NOT_BOUND_ENTERPRISE(40901, "设备未绑定企业", HttpStatus.CONFLICT),
    DEVICE_BIND_PENDING(40902, "设备绑定申请审核中", HttpStatus.CONFLICT),
    DEVICE_BIND_REJECTED(40903, "设备绑定申请已拒绝", HttpStatus.CONFLICT),
    DEVICE_BIND_EXPIRED(40904, "设备绑定申请已过期", HttpStatus.CONFLICT),
    DEVICE_NOT_BOUND_VEHICLE(40905, "设备未绑定车辆", HttpStatus.CONFLICT),
    VEHICLE_ALREADY_BOUND(40906, "车辆已被其他设备占用", HttpStatus.CONFLICT),
    DEVICE_ACTIVE_SESSION_CONFLICT(40907, "设备存在活动会话，禁止调整绑定", HttpStatus.CONFLICT),
    ENTERPRISE_ACTIVATION_CODE_NOT_FOUND(40908, "企业激活码不存在", HttpStatus.CONFLICT),
    ENTERPRISE_ACTIVATION_CODE_EXPIRED(40909, "企业激活码已过期", HttpStatus.CONFLICT),
    ENTERPRISE_ACTIVATION_CODE_DISABLED(40910, "企业激活码已停用", HttpStatus.CONFLICT),
    DEVICE_BOUND_TO_OTHER_ENTERPRISE(40911, "设备已绑定其他企业", HttpStatus.CONFLICT),
    DEVICE_NOT_ACTIVATED(40912, "设备未激活", HttpStatus.CONFLICT),
    UNAUTHORIZED(40101, "未授权或token失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40301, "无权限访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(40401, "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(40501, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_ERROR(50001, "内部服务器错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ApiCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
