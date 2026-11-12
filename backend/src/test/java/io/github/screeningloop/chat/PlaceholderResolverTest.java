package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaceholderResolverTest {

    private final PlaceholderResolver resolver = new PlaceholderResolver();

    @Test
    void replacesAllowedPlaceholdersWithTheirValues() {
        String resolved = resolver.resolve(
                "Seen on {{FIT_DATE}}: {{FIT_RESULT_LABEL}}.",
                Map.of("FIT_DATE", "13 October 2026", "FIT_RESULT_LABEL", "positive"),
                Set.of("FIT_DATE", "FIT_RESULT_LABEL"));
        assertThat(resolved).isEqualTo("Seen on 13 October 2026: positive.");
    }

    @Test
    void refusesAPlaceholderThatIsNotOnTheAllowlist() {
        assertThatThrownBy(() -> resolver.resolve(
                "Phone: {{PATIENT_PHONE}}", Map.of("PATIENT_PHONE", "555"), Set.of("FIT_DATE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PATIENT_PHONE");
    }

    @Test
    void refusesAnAllowedPlaceholderWithoutAValue() {
        assertThatThrownBy(() -> resolver.resolve("On {{FIT_DATE}}", Map.of(), Set.of("FIT_DATE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIT_DATE");
    }

    @Test
    void leavesTextWithoutPlaceholdersUntouched() {
        assertThat(resolver.resolve("Hello.", Map.of(), Set.of())).isEqualTo("Hello.");
    }

    @Test
    void doesNotReinterpretPlaceholdersInsideValues() {
        String resolved = resolver.resolve(
                "Medications: {{MEDICATION_LIST}}", Map.of("MEDICATION_LIST", "{{FIT_DATE}} $1"),
                Set.of("MEDICATION_LIST"));
        assertThat(resolved).isEqualTo("Medications: {{FIT_DATE}} $1");
    }

    @Test
    void listsThePlaceholdersOfATemplate() {
        assertThat(resolver.placeholdersIn("A {{X}} and {{Y}} and {{X}}")).containsExactlyInAnyOrder("X", "Y");
    }
}
