package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.patient.PatientSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatComposerRewriteTest {

    private static final int AGE = 50;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    private final List<String> received = new ArrayList<>();

    private ChatComposer composerRewritingWith(String candidate) {
        return new ChatComposer(CLOCK, template -> {
            received.add(template);
            return Optional.ofNullable(candidate);
        });
    }

    private PatientSummary summary(CaseStage stage, String dueDate, List<String> medications) {
        CaseView view = new CaseView("t1", "1", "Patient/p1", "Observation/fit-1", stage.code(), "in-progress",
                "Practitioner/gp-1", dueDate);
        return new PatientSummary("p1", "Ana Greenholt", "female", AGE, List.of(), List.of(), medications,
                List.of(), List.of(view));
    }

    @Test
    void usesAValidRewriteAndLetsTheBackendSubstituteTheRealValues() {
        String candidate = "Your care team has asked a specialist to see you, and hopes to hear back "
                + "by {{NEXT_STEP_DATE}}.";
        String reply = composerRewritingWith(candidate)
                .compose(Intent.CASE_STATUS, summary(CaseStage.REFERRAL_REQUESTED, "2026-12-01", List.of()));
        assertThat(reply).isEqualTo(
                "Your care team has asked a specialist to see you, and hopes to hear back by 1 December 2026.");
    }

    @Test
    void sendsTheModelOnlyTheTemplateWithOpaquePlaceholders() {
        composerRewritingWith(null)
                .compose(Intent.CASE_STATUS, summary(CaseStage.REFERRAL_REQUESTED, "2026-12-01", List.of()));
        assertThat(received).singleElement().satisfies(sent -> {
            assertThat(sent).contains("{{NEXT_STEP_DATE}}");
            assertThat(sent).doesNotContain("2026").doesNotContain("Ana").doesNotContain("Greenholt");
        });
    }

    @Test
    void sendsMedicationNamesOnlyAsAPlaceholder() {
        composerRewritingWith(null)
                .compose(Intent.MEDICATIONS, summary(CaseStage.CLOSED, null, List.of("Aspirin 81 MG")));
        assertThat(received).singleElement().satisfies(sent -> {
            assertThat(sent).contains("{{MEDICATION_LIST}}").doesNotContain("Aspirin");
        });
    }

    @Test
    void fallsBackToTheTemplateWhenTheRewriteDropsAPlaceholderOrInventsANumber() {
        PatientSummary summary = summary(CaseStage.REFERRAL_REQUESTED, "2026-12-01", List.of());
        String fixed = new ChatComposer(CLOCK).compose(Intent.CASE_STATUS, summary);
        assertThat(composerRewritingWith("Your referral was requested.").compose(Intent.CASE_STATUS, summary))
                .isEqualTo(fixed);
        assertThat(composerRewritingWith("We will call in 3 days, by {{NEXT_STEP_DATE}}.")
                .compose(Intent.CASE_STATUS, summary)).isEqualTo(fixed);
    }

    @Test
    void fallsBackToTheTemplateWhenThereIsNoRewrite() {
        PatientSummary summary = summary(CaseStage.REFERRAL_REQUESTED, "2026-12-01", List.of());
        assertThat(composerRewritingWith(null).compose(Intent.CASE_STATUS, summary))
                .isEqualTo(new ChatComposer(CLOCK).compose(Intent.CASE_STATUS, summary));
    }

    @Test
    void neverAsksTheModelToRewriteFixedTexts() {
        ChatComposer composer = composerRewritingWith("Anything.");
        composer.compose(Intent.GREETING, null);
        composer.compose(Intent.HELP, null);
        composer.compose(Intent.UNSUPPORTED, null);
        composer.compose(Intent.CASE_STATUS, new PatientSummary(
                "p1", "Ana", "female", AGE, List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThat(received).isEmpty();
    }
}
