package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.patient.PatientSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatComposerTest {

    private static final int AGE = 50;
    private static final double FIT_VALUE = 488.52;

    private final ChatComposer composer = new ChatComposer(
            Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC));

    private CaseView caseAt(CaseStage stage, String dueDate) {
        return new CaseView("t1", "1", "Patient/p1", "Observation/fit-1", stage.code(), "in-progress",
                "Practitioner/gp-1", dueDate);
    }

    private PatientSummary summary(
            List<PatientSummary.FitResult> fit, List<String> medications,
            List<PatientSummary.ProcedureEntry> procedures, List<CaseView> cases) {
        return new PatientSummary("p1", "Ana Greenholt", "female", AGE, fit, List.of(), medications, procedures, cases);
    }

    private PatientSummary withCase(CaseStage stage) {
        return summary(List.of(), List.of(), List.of(), List.of(caseAt(stage, "2026-11-21")));
    }

    @Test
    void hasAReassuringTemplateForEveryStageOfTheCase() {
        for (CaseStage stage : CaseStage.values()) {
            String reply = composer.compose(Intent.CASE_STATUS, withCase(stage));
            assertThat(reply).as(stage.code()).isNotBlank().doesNotContain("{{").doesNotContain("}}");
        }
    }

    @Test
    void tellsThePatientTheDateOfTheNextStepWhenTheCaseIsOpen() {
        String reply = composer.compose(Intent.CASE_STATUS, withCase(CaseStage.APPOINTMENT_BOOKED));
        assertThat(reply).contains("21 November 2026");
    }

    @Test
    void admitsThatAnOpenCaseIsTakingLongerWhenItsPlannedDateHasPassed() {
        PatientSummary overdue = summary(List.of(), List.of(), List.of(),
                List.of(caseAt(CaseStage.REFERRAL_REQUESTED, "2026-11-06")));
        String reply = composer.compose(Intent.CASE_STATUS, overdue);
        assertThat(reply).contains("referral requested").contains("6 November 2026")
                .contains("taking longer than planned").contains("contact your care team");
        assertThat(reply).doesNotContain("expected by").doesNotContain("aiming for");
    }

    @Test
    void doesNotTreatADateEqualToTodayAsOverdue() {
        PatientSummary dueToday = summary(List.of(), List.of(), List.of(),
                List.of(caseAt(CaseStage.REFERRAL_REQUESTED, "2026-11-12")));
        assertThat(composer.compose(Intent.CASE_STATUS, dueToday)).doesNotContain("taking longer");
    }

    @Test
    void doesNotPromiseADateWhenTheCaseIsClosed() {
        String reply = composer.compose(Intent.CASE_STATUS, withCase(CaseStage.CLOSED));
        assertThat(reply).doesNotContain("2026");
    }

    @Test
    void explainsThatAPositiveResultIsNotADiagnosis() {
        String reply = composer.compose(Intent.CASE_STATUS, withCase(CaseStage.RESULT_ISSUED));
        assertThat(reply).containsIgnoringCase("not a diagnosis");
    }

    @Test
    void saysSoWhenThereIsNoCaseOnRecord() {
        String reply = composer.compose(Intent.CASE_STATUS, summary(List.of(), List.of(), List.of(), List.of()));
        assertThat(reply).containsIgnoringCase("do not see");
    }

    @Test
    void talksAboutTheOpenCaseBeforeAClosedOne() {
        PatientSummary summary = summary(List.of(), List.of(), List.of(),
                List.of(caseAt(CaseStage.CLOSED, null), caseAt(CaseStage.REFERRAL_REQUESTED, "2026-12-01")));
        assertThat(composer.compose(Intent.CASE_STATUS, summary)).contains("1 December 2026");
    }

    @Test
    void listsTheActiveMedicationsOrSaysThereAreNone() {
        PatientSummary with = summary(List.of(), List.of("Aspirin 81 MG", "Metformin"), List.of(), List.of());
        assertThat(composer.compose(Intent.MEDICATIONS, with)).contains("Aspirin 81 MG, Metformin");
        PatientSummary without = summary(List.of(), List.of(), List.of(), List.of());
        assertThat(composer.compose(Intent.MEDICATIONS, without)).containsIgnoringCase("do not see");
    }

    @Test
    void reportsTheLatestFitResultWithoutTheNumber() {
        PatientSummary summary = summary(
                List.of(new PatientSummary.FitResult("2026-10-13T00:59:39-05:00", FIT_VALUE, "ng/mL", "POS")),
                List.of(), List.of(new PatientSummary.ProcedureEntry("Colonoscopy (procedure)", "2026-11-01")),
                List.of());
        String reply = composer.compose(Intent.LATEST_EXAMS, summary);
        assertThat(reply).contains("13 October 2026").containsIgnoringCase("positive")
                .doesNotContain("488").contains("Colonoscopy (procedure)");
    }

    @Test
    void saysSoWhenThereAreNoExams() {
        String reply = composer.compose(Intent.LATEST_EXAMS, summary(List.of(), List.of(), List.of(), List.of()));
        assertThat(reply).containsIgnoringCase("do not see");
    }

    @Test
    void usesFixedTextsWithoutPatientDataForTheOtherIntents() {
        assertThat(composer.compose(Intent.GREETING, null)).isNotBlank();
        assertThat(composer.compose(Intent.HELP, null)).contains("medications");
        assertThat(composer.compose(Intent.UNSUPPORTED, null)).containsIgnoringCase("care team");
    }
}
