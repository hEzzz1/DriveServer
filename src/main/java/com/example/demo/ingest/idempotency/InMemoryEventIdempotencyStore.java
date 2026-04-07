package com.example.demo.ingest.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "ingest.idempotency.store", havingValue = "memory")
public class InMemoryEventIdempotencyStore implements EventIdempotencyStore {

    private final Map<String, Long> keyExpireAtMs = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String eventId, Duration ttl) {
        long now = System.currentTimeMillis();
        long expireAt = now + ttl.toMillis();
        AtomicBoolean acquired = new AtomicBoolean(false);

        keyExpireAtMs.compute(eventId, (key, currentExpireAt) -> {
            if (currentExpireAt == null || currentExpireAt <= now) {
                acquired.set(true);
                return expireAt;
            }
            return currentExpireAt;
        });

        cleanupIfNeeded(now);
        return acquired.get();
    }

    @Override
    public void release(String eventId) {
        keyExpireAtMs.remove(eventId);
    }

    private void cleanupIfNeeded(long now) {
        if (keyExpireAtMs.size() < 1024) {
            return;
        }
        keyExpireAtMs.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
