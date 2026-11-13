package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicianChatServiceTest {

    private static final String PATIENT_ID = "p1";
    private static final String AGENT = "Practitioner/demo-gp-1";
    private static final int AGE = 64;
    private static final double FIT_VALUE = 267.2;

    private final PatientSummaryService summaries = mock(PatientSummaryService.class);
    private final ClinicianChatService service = new ClinicianChatService(summaries);

    private PatientSummary summary() {
        return new PatientSummary(
                PATIENT_ID, "Ana Greenholt", "female", AGE,
                List.of(new PatientSummary.FitResult("2022-07-05T10:00:00Z", FIT_VALUE, "ng/mL", "POS")),
                List.of(new PatientSummary.ConditionEntry("Polyp of colon (disorder)", "resolved")),
                List.of("Aspirin 81 MG"),
                List.of(new PatientSummary.ProcedureEntry("Colonoscopy (procedure)", "2022-08-01T09:00:00Z")),
                List.of(new CaseView("t1", "1", "Patient/p1", "Observation/o1", "referral-rejected", "in-progress",
                        "Practitioner/demo-gp-1", "2026-12-01")));
    }

    private ChatReply ask(String text) {
        when(summaries.summarize(PATIENT_ID, AGENT)).thenReturn(summary());
        return service.answer(PATIENT_ID, AGENT, text);
    }

    @Test
    void answersMedicationsFromTheRecord() {
        ChatReply reply = ask("active medications?");
        assertThat(reply.intent()).isEqualTo("MEDICATIONS");
        assertThat(reply.reply()).contains("Aspirin 81 MG");
        verify(summaries).summarize(PATIENT_ID, AGENT);
    }

    @Test
    void answersFitResultsWithValueUnitAndInterpretation() {
        ChatReply reply = ask("show the FIT results");
        assertThat(reply.intent()).isEqualTo("FIT_RESULTS");
        assertThat(reply.reply()).contains("2022-07-05").contains("267.2 ng/mL").contains("POS");
    }

    @Test
    void answersConditionsProceduresAndCases() {
        assertThat(ask("conditions").reply()).contains("Polyp of colon (disorder) (resolved)");
        assertThat(ask("recent procedures").reply()).contains("2022-08-01: Colonoscopy (procedure)");
        ChatReply cases = ask("case status");
        assertThat(cases.intent()).isEqualTo("CASES");
        assertThat(cases.reply()).contains("referral-rejected").contains("demo-gp-1").contains("2026-12-01");
    }

    @Test
    void givesAShortSummaryOfThePatient() {
        ChatReply reply = ask("give me a summary");
        assertThat(reply.intent()).isEqualTo("SUMMARY");
        assertThat(reply.reply()).contains("Ana Greenholt").contains("64").contains("female");
    }

    @Test
    void refusesToChangeDataOrGiveClinicalAdviceWithoutReadingTheRecord() {
        for (String text : List.of("close the case", "update the owner", "should we start chemo?",
                "what is the diagnosis", "prescribe aspirin")) {
            ChatReply reply = service.answer(PATIENT_ID, AGENT, text);
            assertThat(reply.intent()).as(text).isEqualTo("UNSUPPORTED");
            assertThat(reply.reply()).contains("read-only").contains("form");
        }
        verifyNoInteractions(summaries);
    }

    @Test
    void explainsWhatItCanShowWhenAskedForHelp() {
        assertThat(service.answer(PATIENT_ID, AGENT, "help").intent()).isEqualTo("HELP");
        verifyNoInteractions(summaries);
    }

    @Test
    void saysSoWhenTheRecordHasNothingToShow() {
        PatientSummary empty = new PatientSummary(PATIENT_ID, "Ana", "female", AGE, List.of(), List.of(), List.of(),
                List.of(), List.of());
        when(summaries.summarize(PATIENT_ID, AGENT)).thenReturn(empty);
        assertThat(service.answer(PATIENT_ID, AGENT, "medications").reply()).containsIgnoringCase("no active");
    }
}
