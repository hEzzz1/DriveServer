package com.example.demo.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("auth.jwt.secret must not be empty");
        }
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret length must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtTokenResult issueToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        List<String> normalizedRoles = roles == null ? List.of() : roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        String token = Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("uid", userId)
                .claim("roles", normalizedRoles)
                .signWith(signingKey)
                .compact();

        return new JwtTokenResult(token, expireAt);
    }

    public AuthenticatedUser parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = toLong(claims.get("uid"));
        String username = claims.getSubject();
        List<String> roles = toRoles(claims.get("roles"));
        return new AuthenticatedUser(userId, username, roles);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("Invalid uid claim");
    }

    private List<String> toRoles(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof String role && StringUtils.hasText(role)) {
                roles.add(role.trim().toUpperCase());
            }
        }
        return roles.stream().filter(Objects::nonNull).distinct().toList();
    }
}
