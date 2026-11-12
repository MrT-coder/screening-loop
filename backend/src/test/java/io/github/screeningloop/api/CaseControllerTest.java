package io.github.screeningloop.api;

import static io.github.screeningloop.security.TestUsers.PRACTITIONER;
import static io.github.screeningloop.security.TestUsers.coordinator;
import static io.github.screeningloop.security.TestUsers.coordinatorActor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import io.github.screeningloop.access.ForbiddenActionException;
import io.github.screeningloop.cases.CaseCodes;
import io.github.screeningloop.cases.CaseService;
import io.github.screeningloop.cases.CaseTaskMapper;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import io.github.screeningloop.cases.CaseWithHistory;
import io.github.screeningloop.cases.InvalidTransitionException;
import io.github.screeningloop.cases.TransitionRequest;
import io.github.screeningloop.security.SecurityConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CaseController.class)
@Import(SecurityConfiguration.class)
class CaseControllerTest {

    private static final LocalDate DUE_DATE = LocalDate.parse("2026-12-01");
    private static final String OPEN_BODY = """
            {"patient":"Patient/p1","fitResult":"Observation/fit-1","owner":"Practitioner/gp-1","dueDate":"2026-12-01"}
            """;
    private static final String TRANSITION_BODY = """
            {"target":"patient-notified","owner":"Practitioner/nurse-2","dueDate":"2026-12-05"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CaseService service;

    @Test
    void opensACaseAndReturnsItsView() throws Exception {
        when(service.openCase("Patient/p1", "Observation/fit-1", PRACTITIONER, DUE_DATE, coordinatorActor()))
                .thenReturn(storedTask());
        mvc.perform(post("/api/cases").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(OPEN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.stage").value("result-issued"))
                .andExpect(jsonPath("$.owner").value(PRACTITIONER))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));
    }

    @Test
    void rejectsOpeningACaseWithoutOwner() throws Exception {
        String body = OPEN_BODY.replace("Practitioner/gp-1", "");
        mvc.perform(post("/api/cases").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void advancesACaseOnBehalfOfTheAuthenticatedActor() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(coordinatorActor())))
                .thenReturn(storedTask());
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t1"));
    }

    @Test
    void answersUnprocessableWhenTheTransitionIsNotAllowed() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(coordinatorActor())))
                .thenThrow(new InvalidTransitionException("Transition is not allowed"));
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Transition is not allowed"));
    }

    @Test
    void answersUnprocessableForAnUnknownTargetStage() throws Exception {
        String body = TRANSITION_BODY.replace("patient-notified", "teleported");
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void answersUnprocessableForAnUnknownClosureReason() throws Exception {
        String body = """
                {"target":"closed-without-care","owner":"Practitioner/gp-1","reason":"teleported"}
                """;
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void answersForbiddenWhenThePolicyDeniesTheTransition() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(coordinatorActor())))
                .thenThrow(new ForbiddenActionException("A clinician can only advance the cases they own"));
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("A clinician can only advance the cases they own"));
    }

    @Test
    void answersConflictWhenSomeoneElseChangedTheCase() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(coordinatorActor())))
                .thenThrow(new ResourceVersionConflictException("stale"));
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void answersBadRequestWithoutLeakingTheServerMessageWhenAReferenceDoesNotExist() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(coordinatorActor())))
                .thenThrow(new InvalidRequestException("HAPI-1094: Resource Practitioner/ghost not found"));
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "The request refers to a resource that does not exist or is not valid."));
    }

    @Test
    void returnsTheCaseDetailWithItsHistory() throws Exception {
        Provenance provenance = new Provenance();
        provenance.setRecorded(Date.from(Instant.parse("2026-11-01T10:00:00Z")));
        provenance.setActivity(new CodeableConcept(new Coding(CaseCodes.STAGE_SYSTEM, "patient-notified", null)));
        provenance.addAgent().setWho(new Reference(PRACTITIONER));
        when(service.findCase("t1", PRACTITIONER)).thenReturn(new CaseWithHistory(storedTask(), List.of(provenance)));
        mvc.perform(get("/api/cases/t1").with(user(coordinator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.id").value("t1"))
                .andExpect(jsonPath("$.history[0].stage").value("patient-notified"))
                .andExpect(jsonPath("$.history[0].agents[0]").value(PRACTITIONER));
    }

    @Test
    void answersNotFoundForAnUnknownCase() throws Exception {
        when(service.findCase("ghost", PRACTITIONER)).thenThrow(new ResourceNotFoundException("missing"));
        mvc.perform(get("/api/cases/ghost").with(user(coordinator()))).andExpect(status().isNotFound());
    }

    @Test
    void listsOpenCases() throws Exception {
        when(service.findOpenCases(PRACTITIONER)).thenReturn(List.of(storedTask()));
        mvc.perform(get("/api/cases").with(user(coordinator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"));
    }

    @Test
    void answersUnauthorizedWithoutASession() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cases/t1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cases/t1/transitions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAStateChangingRequestWithoutACsrfToken() throws Exception {
        mvc.perform(post("/api/cases/t1/transitions").with(user(coordinator()))
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isForbidden());
    }

    private Task storedTask() {
        Task task = new CaseTaskMapper(new CaseTransitionPolicy()).openCase(
                "Patient/p1", "Observation/fit-1", PRACTITIONER, DUE_DATE, Instant.EPOCH);
        task.setId("t1");
        task.getMeta().setVersionId("1");
        return task;
    }
}
