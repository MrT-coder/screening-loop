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

class ChatComposerLanguageTest {

    private static final int AGE = 50;
    private static final double FIT_VALUE = 488.52;
    private static final String SPANISH_REASSURANCE = ReplyTemplates.of(Language.ES).reassurancePhrase();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    private final ChatComposer composer = new ChatComposer(CLOCK);

    private PatientSummary summary(List<PatientSummary.FitResult> fit, List<CaseView> cases) {
        return new PatientSummary("p1", "Ana Greenholt", "female", AGE, fit, List.of(), List.of(), List.of(), cases);
    }

    private PatientSummary withCase(CaseStage stage, String dueDate) {
        CaseView view = new CaseView("t1", "1", "Patient/p1", "Observation/fit-1", stage.code(), "in-progress",
                "Practitioner/gp-1", dueDate);
        return summary(List.of(), List.of(view));
    }

    private PatientSummary withPositiveFit() {
        return summary(List.of(new PatientSummary.FitResult("2026-10-13T00:59:39-05:00", FIT_VALUE, "ng/mL", "POS")),
                List.of());
    }

    @Test
    void answersGreetingsAndRefusalsInSpanish() {
        assertThat(composer.compose(Intent.GREETING, null, Language.ES)).startsWith("Hola.");
        assertThat(composer.compose(Intent.UNSUPPORTED, null, Language.ES))
                .contains("Solo puedo ayudarle").contains("equipo de atenci");
    }

    @Test
    void formatsTheNextStepDateInSpanish() {
        String reply = composer.compose(Intent.CASE_STATUS,
                withCase(CaseStage.APPOINTMENT_BOOKED, "2026-11-21"), Language.ES);
        assertThat(reply).contains("agendada").contains("21 de noviembre de 2026");
    }

    @Test
    void usesTheSpanishStageLabelWhenTheCaseIsOverdue() {
        String reply = composer.compose(Intent.CASE_STATUS,
                withCase(CaseStage.REFERRAL_REQUESTED, "2026-11-06"), Language.ES);
        assertThat(reply).contains("solicitada").contains("6 de noviembre de 2026")
                .contains("de lo planeado");
    }

    @Test
    void resolvesEveryPlaceholderForEveryStageInSpanish() {
        for (CaseStage stage : CaseStage.values()) {
            String reply = composer.compose(Intent.CASE_STATUS, withCase(stage, "2026-11-21"), Language.ES);
            assertThat(reply).as(stage.code()).isNotBlank().doesNotContain("{{").doesNotContain("}}");
        }
    }

    @Test
    void tellsTheFitResultIsNotADiagnosisInSpanish() {
        String reply = composer.compose(Intent.LATEST_EXAMS, withPositiveFit(), Language.ES);
        assertThat(reply).contains("13 de octubre de 2026").contains(SPANISH_REASSURANCE);
    }

    @Test
    void keepsEnglishAsTheDefaultLanguage() {
        assertThat(composer.compose(Intent.GREETING, null)).startsWith("Hello.");
    }

    @Test
    void tellsTheRewriterWhichLanguageToUse() {
        List<Language> seen = new ArrayList<>();
        ReplyRewriter rewriter = new ReplyRewriter() {
            @Override
            public Optional<String> rewrite(String template) {
                return Optional.empty();
            }

            @Override
            public Optional<String> rewrite(String template, Language language) {
                seen.add(language);
                return Optional.empty();
            }
        };
        new ChatComposer(CLOCK, rewriter).compose(Intent.LATEST_EXAMS, withPositiveFit(), Language.ES);
        assertThat(seen).containsExactly(Language.ES);
    }

    @Test
    void rejectsASpanishRewriteThatDropsTheReassurance() {
        ReplyRewriter dropsReassurance = template -> Optional.of("Su prueba dio positivo el {{FIT_DATE}}.");
        String reply = new ChatComposer(CLOCK, dropsReassurance)
                .compose(Intent.LATEST_EXAMS, withPositiveFit(), Language.ES);
        assertThat(reply).contains(SPANISH_REASSURANCE);
    }

    @Test
    void acceptsASpanishRewriteThatKeepsReassuranceAndPlaceholders() {
        ReplyRewriter keeps = template ->
                Optional.of("El {{FIT_DATE}} se hizo su prueba: dio positivo, y eso " + SPANISH_REASSURANCE + ".");
        String reply = new ChatComposer(CLOCK, keeps).compose(Intent.LATEST_EXAMS, withPositiveFit(), Language.ES);
        assertThat(reply).startsWith("El 13 de octubre de 2026 se hizo su prueba");
    }
}
