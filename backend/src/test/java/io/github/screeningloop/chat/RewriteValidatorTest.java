package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RewriteValidatorTest {

    private static final String ORIGINAL =
            "Your stool test (FIT) was positive. A positive result is not a diagnosis. "
                    + "Your care team aims to contact you by {{NEXT_STEP_DATE}}.";
    private static final int PADDING_REPEATS = 100;
    private static final Set<String> ALLOWED = Set.of("NEXT_STEP_DATE", "STAGE_LABEL");

    private final RewriteValidator validator = new RewriteValidator(new PlaceholderResolver());

    private boolean isValid(String candidate) {
        return validator.isValid(ORIGINAL, candidate, ALLOWED);
    }

    @Test
    void acceptsAWarmerSentenceThatKeepsThePlaceholderAndTheReassurance() {
        assertThat(isValid("Your stool test (FIT) came back positive, which is not a diagnosis. "
                + "Your care team plans to reach you by {{NEXT_STEP_DATE}}.")).isTrue();
    }

    @Test
    void rejectsACandidateThatDropsAPlaceholder() {
        assertThat(isValid("Your stool test was positive. A positive result is not a diagnosis.")).isFalse();
    }

    @Test
    void rejectsACandidateThatInventsAPlaceholderOrUsesOneThatIsNotAllowed() {
        assertThat(isValid("A positive result is not a diagnosis. Call {{PATIENT_PHONE}} by {{NEXT_STEP_DATE}}."))
                .isFalse();
    }

    @Test
    void rejectsACandidateWithAnyDigit() {
        assertThat(isValid("A positive result is not a diagnosis. We will call within 3 days, "
                + "by {{NEXT_STEP_DATE}}.")).isFalse();
    }

    @Test
    void rejectsACandidateThatDropsTheNotADiagnosisReassurance() {
        assertThat(isValid("Your stool test was positive, so you need a follow-up. "
                + "Your care team will contact you by {{NEXT_STEP_DATE}}.")).isFalse();
    }

    @Test
    void rejectsLinksMarkupContactDetailsAndMultipleParagraphs() {
        String base = "A positive result is not a diagnosis. By {{NEXT_STEP_DATE}}.";
        assertThat(isValid(base + " See https://example.org")).isFalse();
        assertThat(isValid(base + " Write to help@example.org")).isFalse();
        assertThat(isValid(base + " <b>Important</b>")).isFalse();
        assertThat(isValid(base + "\n\nSecond paragraph.")).isFalse();
    }

    @Test
    void rejectsAnExcessivelyLongCandidate() {
        String padding = "Please take care. ".repeat(PADDING_REPEATS);
        assertThat(isValid("A positive result is not a diagnosis. By {{NEXT_STEP_DATE}}. " + padding)).isFalse();
    }

    @Test
    void rejectsABlankCandidate() {
        assertThat(isValid("   ")).isFalse();
        assertThat(isValid(null)).isFalse();
    }
}
