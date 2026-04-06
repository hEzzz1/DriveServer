package com.example.demo.common.exception;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ApiCode code = ex.getApiCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.error(code, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(ApiCode.INVALID_PARAM.getHttpStatus())
                .body(ApiResponse.error(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex) {
        return ResponseEntity.status(ApiCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ApiCode.FORBIDDEN, ApiCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(ApiCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(ApiCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.getMessage()));
    }
}
