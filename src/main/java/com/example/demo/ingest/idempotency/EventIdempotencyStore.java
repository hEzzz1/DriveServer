package com.example.demo.ingest.idempotency;

import java.time.Duration;

public interface EventIdempotencyStore {
    boolean tryAcquire(String eventId, Duration ttl);

    void release(String eventId);
}
