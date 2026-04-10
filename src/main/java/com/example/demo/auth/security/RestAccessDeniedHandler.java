package com.example.demo.auth.security;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.trace.TraceIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("Access denied: traceId={} method={} path={} reason={}",
                TraceIdContext.getTraceId(),
                request.getMethod(),
                request.getRequestURI(),
                shortReason(accessDeniedException));
        response.setStatus(ApiCode.FORBIDDEN.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(ApiCode.FORBIDDEN, ApiCode.FORBIDDEN.getMessage()));
    }

    private String shortReason(AccessDeniedException ex) {
        String className = ex.getClass().getSimpleName();
        String message = ex.getMessage();
        if (StringUtils.hasText(message)) {
            return className + ": " + message;
        }
        return className;
    }
}
