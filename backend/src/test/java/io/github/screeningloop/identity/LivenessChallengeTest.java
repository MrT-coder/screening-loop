package io.github.screeningloop.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LivenessChallengeTest {

    private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");
    private static final Duration VALIDITY = Duration.ofMinutes(2);

    private final MutableClock clock = new MutableClock(START);
    private final LivenessChallenge challenge = new LivenessChallenge(clock, VALIDITY);

    @Test
    void acceptsAnIssuedTokenExactlyOnce() {
        String token = challenge.issue();
        assertThat(challenge.consume(token)).isTrue();
        assertThat(challenge.consume(token)).isFalse();
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = challenge.issue();
        clock.advance(VALIDITY.plusSeconds(1));
        assertThat(challenge.consume(token)).isFalse();
    }

    @Test
    void rejectsUnknownOrMissingTokens() {
        assertThat(challenge.consume("made-up")).isFalse();
        assertThat(challenge.consume(null)).isFalse();
    }

    @Test
    void issuesDifferentTokensEachTime() {
        assertThat(challenge.issue()).isNotEqualTo(challenge.issue());
    }
}
