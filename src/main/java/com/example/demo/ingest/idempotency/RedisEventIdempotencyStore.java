package com.example.demo.ingest.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "ingest.idempotency.store", havingValue = "redis", matchIfMissing = true)
public class RedisEventIdempotencyStore implements EventIdempotencyStore {

    private static final String IDEMPOTENCY_KEY_PREFIX = "ingest:idempotency:event:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisEventIdempotencyStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquire(String eventId, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENCY_KEY_PREFIX + eventId, "1", ttl);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void release(String eventId) {
        stringRedisTemplate.delete(IDEMPOTENCY_KEY_PREFIX + eventId);
    }
}
