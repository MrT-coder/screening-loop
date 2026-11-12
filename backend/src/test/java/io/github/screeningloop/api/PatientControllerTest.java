package io.github.screeningloop.api;

import static io.github.screeningloop.security.TestUsers.PRACTITIONER;
import static io.github.screeningloop.security.TestUsers.guest;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import io.github.screeningloop.security.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientController.class)
@Import(SecurityConfiguration.class)
class PatientControllerTest {

    private static final int AGE = 64;
    private static final double FIT_VALUE = 267.2;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PatientSummaryService service;

    @Test
    void returnsThePatientSummaryToAnyAuthenticatedRole() throws Exception {
        PatientSummary summary = new PatientSummary(
                "p1", "Ana Greenholt", "female", AGE,
                List.of(new PatientSummary.FitResult("2022-07-05", FIT_VALUE, "ng/mL", "POS")),
                List.of(new PatientSummary.ConditionEntry("Polyp of colon (disorder)", "resolved")),
                List.of("Aspirin 81 MG"),
                List.of(new PatientSummary.ProcedureEntry("Colonoscopy (procedure)", "2022-08-01")),
                List.of());
        when(service.summarize("p1", PRACTITIONER)).thenReturn(summary);
        mvc.perform(get("/api/patients/p1/summary").with(user(guest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Greenholt"))
                .andExpect(jsonPath("$.age").value(AGE))
                .andExpect(jsonPath("$.fitResults[0].interpretation").value("POS"))
                .andExpect(jsonPath("$.activeMedications[0]").value("Aspirin 81 MG"))
                .andExpect(jsonPath("$.recentProcedures[0].name").value("Colonoscopy (procedure)"));
    }

    @Test
    void answersNotFoundForAnUnknownPatient() throws Exception {
        when(service.summarize("ghost", PRACTITIONER)).thenThrow(new ResourceNotFoundException("missing"));
        mvc.perform(get("/api/patients/ghost/summary").with(user(guest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("The requested resource does not exist."));
    }

    @Test
    void ignoresIdsThatAreNotValidFhirIds() throws Exception {
        mvc.perform(get("/api/patients/bad_id/summary").with(user(guest()))).andExpect(status().isNotFound());
    }

    @Test
    void requiresASession() throws Exception {
        mvc.perform(get("/api/patients/p1/summary")).andExpect(status().isUnauthorized());
    }
}
