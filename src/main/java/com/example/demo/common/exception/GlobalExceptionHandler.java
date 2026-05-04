package com.example.demo.common.exception;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.trace.TraceIdContext;
import com.example.demo.system.service.SystemDiagnosticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final SystemDiagnosticsService systemDiagnosticsService;

    public GlobalExceptionHandler(SystemDiagnosticsService systemDiagnosticsService) {
        this.systemDiagnosticsService = systemDiagnosticsService;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ApiCode code = ex.getApiCode();
        recordError(request, code, ex.getMessage(), ex);
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.error(code, ex.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        recordError(request, ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage(), ex);
        return ResponseEntity.status(ApiCode.INVALID_PARAM.getHttpStatus())
                .body(ApiResponse.error(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex, HttpServletRequest request) {
        recordError(request, ApiCode.FORBIDDEN, ApiCode.FORBIDDEN.getMessage(), ex);
        return ResponseEntity.status(ApiCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ApiCode.FORBIDDEN, ApiCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        recordError(request, ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage(), ex);
        return ResponseEntity.status(ApiCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        recordError(request, ApiCode.METHOD_NOT_ALLOWED, ApiCode.METHOD_NOT_ALLOWED.getMessage(), ex);
        return ResponseEntity.status(ApiCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.error(ApiCode.METHOD_NOT_ALLOWED, ApiCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        recordError(request, ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage(), ex);
        return ResponseEntity.status(ApiCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: traceId={} method={} uri={}",
                TraceIdContext.getTraceId(),
                request.getMethod(),
                request.getRequestURI(),
                ex);
        recordError(request, ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.getMessage(), ex);
        return ResponseEntity.status(ApiCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.getMessage()));
    }

    private void recordError(HttpServletRequest request, ApiCode apiCode, String message, Exception exception) {
        try {
            systemDiagnosticsService.record(request, apiCode, message, exception);
        } catch (Exception recordException) {
            log.warn("Failed to record system diagnostic trace: traceId={}", TraceIdContext.getTraceId(), recordException);
        }
    }
}
