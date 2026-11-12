package io.github.screeningloop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentityVerifierTest {

    private static final String DOCUMENT = "999-38-6612";
    private static final LocalDate BIRTH_DATE = LocalDate.parse("1976-10-05");
    private static final String ADDRESS = "203.0.113.7";
    private static final int MAX_FAILURES = 3;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));
    private final LivenessChallenge liveness = new LivenessChallenge(clock, Duration.ofMinutes(2));
    private final AttemptLimiter limiter = new AttemptLimiter(clock, MAX_FAILURES, Duration.ofMinutes(15));
    private final CivilRegistry registry = documentNumber -> Optional.ofNullable(
            Map.of(DOCUMENT, new CivilRegistry.RegistryRecord("3328", BIRTH_DATE)).get(documentNumber));
    private final IdentityVerifier verifier = new IdentityVerifier(registry, liveness, limiter);

    private IdentityClaim claim(String document, LocalDate birthDate) {
        return new IdentityClaim(document, birthDate, liveness.issue());
    }

    @Test
    void returnsThePatientWhenDocumentBirthDateAndLivenessMatch() {
        assertThat(verifier.verify(claim(DOCUMENT, BIRTH_DATE), ADDRESS)).isEqualTo("3328");
    }

    @Test
    void refusesAWrongBirthDateAndAnUnknownDocumentWithTheSameError() {
        assertThatThrownBy(() -> verifier.verify(claim(DOCUMENT, LocalDate.parse("1990-01-01")), ADDRESS))
                .isInstanceOf(IdentityCheckFailedException.class);
        assertThatThrownBy(() -> verifier.verify(claim("000-00-0000", BIRTH_DATE), ADDRESS))
                .isInstanceOf(IdentityCheckFailedException.class);
    }

    @Test
    void refusesWhenTheLivenessTokenIsMissingOrAlreadyUsed() {
        IdentityClaim used = claim(DOCUMENT, BIRTH_DATE);
        verifier.verify(used, ADDRESS);
        assertThatThrownBy(() -> verifier.verify(used, ADDRESS)).isInstanceOf(IdentityCheckFailedException.class);
        assertThatThrownBy(() -> verifier.verify(new IdentityClaim(DOCUMENT, BIRTH_DATE, null), ADDRESS))
                .isInstanceOf(IdentityCheckFailedException.class);
    }

    @Test
    void locksTheDocumentAfterRepeatedFailuresEvenForTheRightBirthDate() {
        for (int attempt = 0; attempt < MAX_FAILURES; attempt++) {
            assertThatThrownBy(() -> verifier.verify(claim(DOCUMENT, LocalDate.parse("1990-01-01")), ADDRESS))
                    .isInstanceOf(IdentityCheckFailedException.class);
        }
        assertThatThrownBy(() -> verifier.verify(claim(DOCUMENT, BIRTH_DATE), ADDRESS))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void locksAnAddressThatKeepsGuessingDifferentDocuments() {
        for (int attempt = 0; attempt < MAX_FAILURES; attempt++) {
            String document = "000-00-000" + attempt;
            assertThatThrownBy(() -> verifier.verify(claim(document, BIRTH_DATE), ADDRESS))
                    .isInstanceOf(IdentityCheckFailedException.class);
        }
        assertThatThrownBy(() -> verifier.verify(claim(DOCUMENT, BIRTH_DATE), ADDRESS))
                .isInstanceOf(TooManyAttemptsException.class);
    }
}
