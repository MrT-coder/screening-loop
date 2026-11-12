package io.github.screeningloop.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttemptLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Clock clock;
    private final int maxFailures;
    private final Duration lockDuration;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    public AttemptLimiter(Clock clock, int maxFailures, Duration lockDuration) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
    }

    public boolean isBlocked(String key) {
        State state = states.get(key);
        if (state == null || state.lockedUntil() == null) {
            return false;
        }
        if (clock.instant().isBefore(state.lockedUntil())) {
            return true;
        }
        states.remove(key);
        return false;
    }

    public void recordFailure(String key) {
        if (states.size() > MAX_TRACKED_KEYS) {
            states.values().removeIf(state -> state.lockedUntil() == null);
        }
        states.compute(key, (name, state) -> {
            int failures = (state == null ? 0 : state.failures()) + 1;
            Instant lockedUntil = failures >= maxFailures ? clock.instant().plus(lockDuration) : null;
            return new State(failures, lockedUntil);
        });
    }

    public void reset(String key) {
        states.remove(key);
    }

    private record State(int failures, Instant lockedUntil) {
    }
}
