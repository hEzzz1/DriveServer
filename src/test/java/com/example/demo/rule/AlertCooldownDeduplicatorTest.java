package com.example.demo.rule;

import com.example.demo.rule.service.AlertCooldownDeduplicator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertCooldownDeduplicatorTest {

    private final AlertCooldownDeduplicator deduplicator = new AlertCooldownDeduplicator();

    @Test
    void shouldSuppressSameMinuteByMinuteBucket() {
        Instant t0 = Instant.parse("2026-04-07T10:00:03Z");

        AlertCooldownDeduplicator.DedupDecision first = deduplicator.checkAndAcquire("veh_001", 1L, t0, 60);
        AlertCooldownDeduplicator.DedupDecision second = deduplicator.checkAndAcquire("veh_001", 1L, t0.plusSeconds(20), 60);

        assertThat(first).isEqualTo(AlertCooldownDeduplicator.DedupDecision.ALLOWED);
        assertThat(second).isEqualTo(AlertCooldownDeduplicator.DedupDecision.BLOCKED_BY_MINUTE_BUCKET);
    }

    @Test
    void shouldSuppressAcrossMinuteWhenCooldownActive() {
        Instant t0 = Instant.parse("2026-04-07T10:00:03Z");

        deduplicator.checkAndAcquire("veh_001", 2L, t0, 120);
        AlertCooldownDeduplicator.DedupDecision blockedByCooldown = deduplicator.checkAndAcquire(
                "veh_001", 2L, t0.plusSeconds(70), 120
        );
        AlertCooldownDeduplicator.DedupDecision recovered = deduplicator.checkAndAcquire(
                "veh_001", 2L, t0.plusSeconds(121), 120
        );

        assertThat(blockedByCooldown).isEqualTo(AlertCooldownDeduplicator.DedupDecision.BLOCKED_BY_COOLDOWN);
        assertThat(recovered).isEqualTo(AlertCooldownDeduplicator.DedupDecision.ALLOWED);
    }
}
