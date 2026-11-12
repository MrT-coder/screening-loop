package io.github.screeningloop.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AttemptLimiterTest {

    private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");
    private static final int MAX_FAILURES = 3;
    private static final Duration LOCK = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock(START);
    private final AttemptLimiter limiter = new AttemptLimiter(clock, MAX_FAILURES, LOCK);

    @Test
    void blocksAKeyAfterTooManyFailures() {
        limiter.recordFailure("doc:1");
        limiter.recordFailure("doc:1");
        assertThat(limiter.isBlocked("doc:1")).isFalse();
        limiter.recordFailure("doc:1");
        assertThat(limiter.isBlocked("doc:1")).isTrue();
    }

    @Test
    void keepsKeysIndependent() {
        for (int attempt = 0; attempt < MAX_FAILURES; attempt++) {
            limiter.recordFailure("doc:1");
        }
        assertThat(limiter.isBlocked("doc:2")).isFalse();
    }

    @Test
    void unblocksOnceTheLockDurationHasPassed() {
        for (int attempt = 0; attempt < MAX_FAILURES; attempt++) {
            limiter.recordFailure("doc:1");
        }
        clock.advance(LOCK.plusSeconds(1));
        assertThat(limiter.isBlocked("doc:1")).isFalse();
    }

    @Test
    void forgetsFailuresAfterAReset() {
        limiter.recordFailure("doc:1");
        limiter.recordFailure("doc:1");
        limiter.reset("doc:1");
        limiter.recordFailure("doc:1");
        assertThat(limiter.isBlocked("doc:1")).isFalse();
    }
}
