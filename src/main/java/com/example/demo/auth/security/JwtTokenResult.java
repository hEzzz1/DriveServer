package com.example.demo.auth.security;

import java.time.Instant;

public record JwtTokenResult(String token, Instant expireAt) {
}
