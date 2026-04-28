package com.example.demo.rule.service;

import com.example.demo.rule.entity.RuleConfig;
import com.example.demo.rule.repository.RuleConfigRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class EdgeConfigVersionResolver {

    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final RuleConfigRepository ruleConfigRepository;
    private volatile CacheEntry cacheEntry;

    public EdgeConfigVersionResolver(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    public String resolveCurrentVersion() {
        CacheEntry cached = cacheEntry;
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.version();
        }

        synchronized (this) {
            cached = cacheEntry;
            now = System.currentTimeMillis();
            if (cached != null && cached.expiresAtMillis() > now) {
                return cached.version();
            }

            String resolved = loadCurrentVersion();
            cacheEntry = new CacheEntry(resolved, now + CACHE_TTL_MILLIS);
            return resolved;
        }
    }

    private String loadCurrentVersion() {
        Object[] summary = ruleConfigRepository.summarizeActiveRuleset();
        long activeCount = toLong(summary, 0);
        if (activeCount <= 0) {
            return "ruleset/empty";
        }

        long maxVersion = toLong(summary, 1);
        LocalDateTime publishedAt = toLocalDateTime(summary, 2);
        long publishedAtEpochSecond = publishedAt == null ? 0L : publishedAt.toEpochSecond(ZoneOffset.UTC);
        return "ruleset/" + activeCount + "/" + maxVersion + "/" + publishedAtEpochSecond;
    }

    private long toLong(Object[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return 0L;
        }
        return ((Number) values[index]).longValue();
    }

    private LocalDateTime toLocalDateTime(Object[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return null;
        }
        Object value = values[index];
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unexpected publishedAt type: " + value.getClass().getName());
    }

    private record CacheEntry(String version, long expiresAtMillis) {
    }
}
