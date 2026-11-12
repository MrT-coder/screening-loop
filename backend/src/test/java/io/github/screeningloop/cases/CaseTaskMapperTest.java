package io.github.screeningloop.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;

class CaseTaskMapperTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 1);
    private static final String PATIENT = "Patient/p1";
    private static final String FIT_RESULT = "Observation/fit-1";
    private static final String OWNER = "Practitioner/gp-1";
    private static final String NEW_OWNER = "Practitioner/nurse-2";
    private static final String AGENT = "Practitioner/gp-1";
    private static final String CURRENT_VERSION = "3";
    private static final String NEXT_VERSION = "4";

    private final CaseTaskMapper mapper = new CaseTaskMapper(new CaseTransitionPolicy());

    @Test
    void opensACaseAtResultIssuedWithOwnerAndDueDate() {
        Task task = mapper.openCase(PATIENT, FIT_RESULT, OWNER, DUE_DATE, NOW);
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.INPROGRESS);
        assertThat(task.getIntent()).isEqualTo(Task.TaskIntent.ORDER);
        assertThat(task.getCode().getCodingFirstRep().getCode()).isEqualTo("screening-case");
        assertThat(task.getFor().getReference()).isEqualTo(PATIENT);
        assertThat(task.getFocus().getReference()).isEqualTo(FIT_RESULT);
        assertThat(stageCode(task)).isEqualTo("result-issued");
        assertThat(task.getOwner().getReference()).isEqualTo(OWNER);
        assertThat(task.getRestriction().getPeriod().getEndElement().getValueAsString()).isEqualTo("2026-12-01");
    }

    @Test
    void refusesToOpenACaseWithoutOwner() {
        assertThatThrownBy(() -> mapper.openCase(PATIENT, FIT_RESULT, "", DUE_DATE, NOW))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void advanceBuildsATransactionWithAGuardedTaskUpdateAndAProvenance() {
        Task current = storedTask();
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, NEW_OWNER, DUE_DATE);
        Bundle bundle = mapper.advance(current, request, AGENT, NOW);
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
        Bundle.BundleEntryRequestComponent put = bundle.getEntry().get(0).getRequest();
        assertThat(put.getMethod()).isEqualTo(Bundle.HTTPVerb.PUT);
        assertThat(put.getUrl()).isEqualTo("Task/t1");
        assertThat(put.getIfMatch()).isEqualTo("W/\"" + CURRENT_VERSION + "\"");
        Task updated = (Task) bundle.getEntry().get(0).getResource();
        assertThat(stageCode(updated)).isEqualTo("patient-notified");
        assertThat(updated.getOwner().getReference()).isEqualTo(NEW_OWNER);
        assertThat(updated.getStatus()).isEqualTo(Task.TaskStatus.INPROGRESS);
    }

    @Test
    void provenanceTargetsTheNextVersionAndIsLinkedFromTheTask() {
        Task current = storedTask();
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, NEW_OWNER, DUE_DATE);
        Bundle bundle = mapper.advance(current, request, AGENT, NOW);
        Bundle.BundleEntryComponent entry = bundle.getEntry().get(1);
        Provenance provenance = (Provenance) entry.getResource();
        assertThat(entry.getRequest().getMethod()).isEqualTo(Bundle.HTTPVerb.POST);
        assertThat(provenance.getTargetFirstRep().getReference()).isEqualTo("Task/t1/_history/" + NEXT_VERSION);
        assertThat(provenance.getAgentFirstRep().getWho().getReference()).isEqualTo(AGENT);
        assertThat(provenance.getActivity().getCodingFirstRep().getCode()).isEqualTo("patient-notified");
        Task updated = (Task) bundle.getEntry().get(0).getResource();
        assertThat(updated.getRelevantHistoryFirstRep().getReference()).isEqualTo(entry.getFullUrl());
    }

    @Test
    void advanceRejectsAnInvalidTransition() {
        Task current = storedTask();
        TransitionRequest request = TransitionRequest.advance(CaseStage.APPOINTMENT_BOOKED, OWNER, DUE_DATE);
        assertThatThrownBy(() -> mapper.advance(current, request, AGENT, NOW))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void closingWithEvidenceCompletesTheTaskAndKeepsTheEvidence() {
        Task current = storedTask(CaseStage.TREATMENT_OR_SURVEILLANCE);
        TransitionRequest request = TransitionRequest.close(OWNER, List.of("DiagnosticReport/biopsy-1"));
        Task updated = (Task) mapper.advance(current, request, AGENT, NOW).getEntry().get(0).getResource();
        assertThat(updated.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(stageCode(updated)).isEqualTo("closed");
        assertThat(((Reference) updated.getOutputFirstRep().getValue()).getReference())
                .isEqualTo("DiagnosticReport/biopsy-1");
    }

    @Test
    void closingWithoutCareCancelsTheTaskAndRecordsTheHumanAgent() {
        Task current = storedTask();
        TransitionRequest request = TransitionRequest.closeWithoutCare(
                OWNER, "Practitioner/supervisor-1", ClosureReason.UNREACHABLE);
        Bundle bundle = mapper.advance(current, request, AGENT, NOW);
        Task updated = (Task) bundle.getEntry().get(0).getResource();
        Provenance provenance = (Provenance) bundle.getEntry().get(1).getResource();
        assertThat(updated.getStatus()).isEqualTo(Task.TaskStatus.CANCELLED);
        Coding reason = updated.getStatusReason().getCodingFirstRep();
        assertThat(reason.getSystem()).isEqualTo(CaseCodes.CLOSURE_REASON_SYSTEM);
        assertThat(reason.getCode()).isEqualTo("unreachable");
        assertThat(provenance.getAgent()).extracting(agent -> agent.getWho().getReference())
                .contains("Practitioner/supervisor-1");
    }

    private String stageCode(Task task) {
        return task.getBusinessStatus().getCodingFirstRep().getCode();
    }

    private Task storedTask() {
        return storedTask(CaseStage.RESULT_ISSUED);
    }

    private Task storedTask(CaseStage stage) {
        Task task = mapper.openCase(PATIENT, FIT_RESULT, OWNER, DUE_DATE, NOW);
        task.getBusinessStatus().getCodingFirstRep().setCode(stage.code());
        task.setId("Task/t1");
        task.getMeta().setVersionId(CURRENT_VERSION);
        return task;
    }
}
