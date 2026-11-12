package io.github.screeningloop.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import io.github.screeningloop.cases.CaseService;
import io.github.screeningloop.cases.CaseTaskMapper;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import io.github.screeningloop.cases.InvalidTransitionException;
import io.github.screeningloop.cases.TransitionRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CaseController.class)
class CaseControllerTest {

    private static final LocalDate DUE_DATE = LocalDate.parse("2026-12-01");
    private static final String ACTOR = "Practitioner/gp-1";
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
        when(service.openCase("Patient/p1", "Observation/fit-1", ACTOR, DUE_DATE))
                .thenReturn(storedTask());
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(OPEN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.stage").value("result-issued"))
                .andExpect(jsonPath("$.owner").value(ACTOR))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));
    }

    @Test
    void rejectsOpeningACaseWithoutOwner() throws Exception {
        String body = OPEN_BODY.replace("Practitioner/gp-1", "");
        mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void advancesACaseOnBehalfOfTheActor() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(ACTOR))).thenReturn(storedTask());
        mvc.perform(post("/api/cases/t1/transitions").header("X-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t1"));
    }

    @Test
    void answersUnprocessableWhenTheTransitionIsNotAllowed() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(ACTOR)))
                .thenThrow(new InvalidTransitionException("Transition is not allowed"));
        mvc.perform(post("/api/cases/t1/transitions").header("X-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Transition is not allowed"));
    }

    @Test
    void answersUnprocessableForAnUnknownTargetStage() throws Exception {
        String body = TRANSITION_BODY.replace("patient-notified", "teleported");
        mvc.perform(post("/api/cases/t1/transitions").header("X-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void answersConflictWhenSomeoneElseChangedTheCase() throws Exception {
        when(service.advance(eq("t1"), any(TransitionRequest.class), eq(ACTOR)))
                .thenThrow(new ResourceVersionConflictException("stale"));
        mvc.perform(post("/api/cases/t1/transitions").header("X-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(TRANSITION_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void listsOpenCasesAndRequiresAnActor() throws Exception {
        when(service.findOpenCases(ACTOR)).thenReturn(List.of(storedTask()));
        mvc.perform(get("/api/cases").header("X-Actor", ACTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"));
        mvc.perform(get("/api/cases")).andExpect(status().isBadRequest());
    }

    private Task storedTask() {
        Task task = new CaseTaskMapper(new CaseTransitionPolicy()).openCase(
                "Patient/p1", "Observation/fit-1", ACTOR, DUE_DATE, Instant.EPOCH);
        task.setId("Task/t1");
        task.getMeta().setVersionId("1");
        return task;
    }
}
