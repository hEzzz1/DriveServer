package com.example.demo.common.api;

import com.example.demo.common.trace.TraceIdContext;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage(), data, currentTraceId());
    }

    public static <T> ApiResponse<T> error(ApiCode apiCode, String message) {
        return new ApiResponse<>(apiCode.getCode(), message, null, currentTraceId());
    }

    private static String currentTraceId() {
        String traceId = TraceIdContext.getTraceId();
        return traceId == null ? "" : traceId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
