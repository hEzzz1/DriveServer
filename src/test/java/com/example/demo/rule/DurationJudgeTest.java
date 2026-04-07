package com.example.demo.rule;

import com.example.demo.rule.service.DurationJudge;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DurationJudgeTest {

    private final DurationJudge durationJudge = new DurationJudge();

    @Test
    void shouldMeetDurationOnCriticalSecond() {
        String vehicleId = "veh_001";
        long ruleId = 1L;
        Instant t0 = Instant.parse("2026-04-07T10:00:00Z");

        boolean at0 = durationJudge.hasReachedDuration(vehicleId, ruleId, t0, true, 3);
        boolean at2 = durationJudge.hasReachedDuration(vehicleId, ruleId, t0.plusSeconds(2), true, 3);
        boolean at3 = durationJudge.hasReachedDuration(vehicleId, ruleId, t0.plusSeconds(3), true, 3);

        assertThat(at0).isFalse();
        assertThat(at2).isFalse();
        assertThat(at3).isTrue();
    }

    @Test
    void shouldResetContinuousDurationWhenThresholdMissed() {
        String vehicleId = "veh_001";
        long ruleId = 2L;
        Instant t0 = Instant.parse("2026-04-07T10:10:00Z");

        durationJudge.hasReachedDuration(vehicleId, ruleId, t0, true, 3);
        durationJudge.hasReachedDuration(vehicleId, ruleId, t0.plusSeconds(2), false, 3);
        boolean afterResetAt2 = durationJudge.hasReachedDuration(vehicleId, ruleId, t0.plusSeconds(4), true, 3);
        boolean afterResetAt3 = durationJudge.hasReachedDuration(vehicleId, ruleId, t0.plusSeconds(7), true, 3);

        assertThat(afterResetAt2).isFalse();
        assertThat(afterResetAt3).isTrue();
    }
}
