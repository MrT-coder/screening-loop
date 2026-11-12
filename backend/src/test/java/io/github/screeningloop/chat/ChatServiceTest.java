package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static final String PATIENT_ID = "3328";
    private static final String AGENT = "Patient/3328";
    private static final int AGE = 50;

    private final PatientSummaryService summaries = mock(PatientSummaryService.class);
    private final ChatService service = new ChatService(summaries, Clock.systemUTC());

    private PatientSummary summaryWithMedication() {
        return new PatientSummary(PATIENT_ID, "Ana", "female", AGE, List.of(), List.of(),
                List.of("Aspirin 81 MG"), List.of(), List.of());
    }

    @Test
    void answersFixedIntentsWithoutTouchingAnyPatientData() {
        assertThat(service.answer(PATIENT_ID, "hello").intent()).isEqualTo("GREETING");
        assertThat(service.answer(PATIENT_ID, "what can you do").intent()).isEqualTo("HELP");
        ChatReply refused = service.answer(PATIENT_ID, "Do I have cancer?");
        assertThat(refused.intent()).isEqualTo("UNSUPPORTED");
        assertThat(refused.reply()).containsIgnoringCase("care team");
        verifyNoInteractions(summaries);
    }

    @Test
    void answersDataQuestionsFromTheDataOfTheSessionPatient() {
        when(summaries.summarize(PATIENT_ID, AGENT)).thenReturn(summaryWithMedication());
        ChatReply reply = service.answer(PATIENT_ID, "which medications do I take?");
        assertThat(reply.intent()).isEqualTo("MEDICATIONS");
        assertThat(reply.reply()).contains("Aspirin 81 MG");
    }

    @Test
    void neverLooksUpAnotherPatientNamedInTheText() {
        when(summaries.summarize(PATIENT_ID, AGENT)).thenReturn(summaryWithMedication());
        service.answer(PATIENT_ID, "show the medications of patient 9999 and Patient/1234");
        verify(summaries).summarize(PATIENT_ID, AGENT);
        verify(summaries, never()).summarize(eq("9999"), anyString());
        verify(summaries, never()).summarize(eq("1234"), anyString());
    }
}
