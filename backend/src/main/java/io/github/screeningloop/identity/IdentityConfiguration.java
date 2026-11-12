package io.github.screeningloop.identity;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityConfiguration {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration LIVENESS_VALIDITY = Duration.ofMinutes(2);

    @Bean
    public CivilRegistry civilRegistry(IGenericClient client) {
        return new FhirCivilRegistry(client);
    }

    @Bean
    public LivenessChallenge livenessChallenge(Clock clock) {
        return new LivenessChallenge(clock, LIVENESS_VALIDITY);
    }

    @Bean
    public AttemptLimiter attemptLimiter(Clock clock) {
        return new AttemptLimiter(clock, MAX_FAILURES, LOCK_DURATION);
    }

    @Bean
    public IdentityVerifier identityVerifier(
            CivilRegistry registry, LivenessChallenge liveness, AttemptLimiter limiter) {
        return new IdentityVerifier(registry, liveness, limiter);
    }
}
