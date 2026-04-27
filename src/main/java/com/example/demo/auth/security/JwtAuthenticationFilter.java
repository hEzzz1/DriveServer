package com.example.demo.auth.security;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   UserAccountRepository userAccountRepository,
                                   ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountRepository = userAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/events".equals(path)
                || "/actuator/health".equals(path)
                || "/actuator/info".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser user = jwtTokenService.parseToken(token);
            validateCurrentUser(user);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT auth failed: method={} path={} errorType={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getClass().getSimpleName());
            writeUnauthorized(response);
        }
    }

    private void validateCurrentUser(AuthenticatedUser authenticatedUser) {
        UserAccount user = userAccountRepository.findById(authenticatedUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!SubjectType.USER.name().equals(user.getSubjectType())) {
            throw new IllegalArgumentException("Unsupported subject type");
        }
        if (user.getStatus() == null || user.getStatus() == (byte) 0) {
            throw new IllegalArgumentException("User disabled");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (StringUtils.hasText(token)) {
                return token;
            }
            return null;
        }

        String tokenFromQuery = request.getParameter("token");
        if (!StringUtils.hasText(tokenFromQuery)) {
            return null;
        }
        return tokenFromQuery.trim();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ApiCode.UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }
}
