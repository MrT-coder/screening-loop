package io.github.screeningloop.identity;

import java.util.List;
import java.util.Optional;

public class IdentityVerifier {

    private final CivilRegistry registry;
    private final LivenessChallenge liveness;
    private final AttemptLimiter limiter;

    public IdentityVerifier(CivilRegistry registry, LivenessChallenge liveness, AttemptLimiter limiter) {
        this.registry = registry;
        this.liveness = liveness;
        this.limiter = limiter;
    }

    public String verify(IdentityClaim claim, String clientAddress) {
        String documentKey = "document:" + claim.documentNumber();
        List<String> keys = List.of(documentKey, "address:" + clientAddress);
        if (keys.stream().anyMatch(limiter::isBlocked)) {
            throw new TooManyAttemptsException();
        }
        boolean isLive = liveness.consume(claim.livenessToken());
        Optional<CivilRegistry.RegistryRecord> record = registry.find(claim.documentNumber());
        boolean isMatch = isLive && record.isPresent() && record.get().birthDate().equals(claim.birthDate());
        if (!isMatch) {
            keys.forEach(limiter::recordFailure);
            throw new IdentityCheckFailedException();
        }
        limiter.reset(documentKey);
        return record.get().patientId();
    }
}
