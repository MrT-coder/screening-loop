package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicianChatLanguageTest {

    private static final int AGE = 50;
    private static final double FIT_VALUE = 488.52;
    private static final String AGENT = "Practitioner/gp-1";

    private final PatientSummaryService summaries = mock(PatientSummaryService.class);
    private final ClinicianChatService service = new ClinicianChatService(summaries);

    private PatientSummary summary() {
        return new PatientSummary("p1", "Ana Greenholt", "female", AGE,
                List.of(new PatientSummary.FitResult("2022-07-05T10:00:00Z", FIT_VALUE, "ng/mL", "POS")),
                List.of(), List.of("Aspirin"), List.of(), List.of());
    }

    @Test
    void answersTheHelpQuestionInSpanish() {
        assertThat(service.answer("p1", AGENT, "ayuda", Language.ES).reply())
                .startsWith("Puedo mostrar los datos registrados");
    }

    @Test
    void understandsSpanishQuestionsAndListsTheDataInSpanish() {
        when(summaries.summarize("p1", AGENT)).thenReturn(summary());
        ChatReply reply = service.answer("p1", AGENT, "medicamentos", Language.ES);
        assertThat(reply.intent()).isEqualTo("MEDICATIONS");
        assertThat(reply.reply()).isEqualTo("Medicamentos activos: Aspirin.");
    }

    @Test
    void summarisesThePatientInSpanish() {
        when(summaries.summarize("p1", AGENT)).thenReturn(summary());
        assertThat(service.answer("p1", AGENT, "resumen", Language.ES).reply())
                .contains("50 a").contains("ltimo FIT: POS").contains("Casos: 0");
    }

    @Test
    void refusesToChangeCasesInSpanishToo() {
        assertThat(service.answer("p1", AGENT, "cerrar el caso", Language.ES).reply())
                .contains("solo lectura");
    }

    @Test
    void keepsEnglishAsTheDefault() {
        when(summaries.summarize("p1", AGENT)).thenReturn(summary());
        assertThat(service.answer("p1", AGENT, "medications").reply()).isEqualTo("Active medications: Aspirin.");
    }
}
