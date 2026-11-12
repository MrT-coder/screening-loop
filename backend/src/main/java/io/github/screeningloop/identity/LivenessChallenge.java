package io.github.screeningloop.identity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LivenessChallenge {

    private static final int TOKEN_BYTES = 32;
    private static final int MAX_PENDING_TOKENS = 10_000;

    private final Clock clock;
    private final Duration validity;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> pending = new ConcurrentHashMap<>();

    public LivenessChallenge(Clock clock, Duration validity) {
        this.clock = clock;
        this.validity = validity;
    }

    public String issue() {
        if (pending.size() > MAX_PENDING_TOKENS) {
            pending.values().removeIf(expiry -> !clock.instant().isBefore(expiry));
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(token, clock.instant().plus(validity));
        return token;
    }

    public boolean consume(String token) {
        if (token == null) {
            return false;
        }
        Instant expiry = pending.remove(token);
        return expiry != null && clock.instant().isBefore(expiry);
    }
}
