package io.github.screeningloop.cases;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;

public class CaseTaskMapper {

    private static final String CLOSED_WITHOUT_CARE_REASON = "Closed without care by a human decision";

    private final CaseTransitionPolicy policy;

    public CaseTaskMapper(CaseTransitionPolicy policy) {
        this.policy = policy;
    }

    public Task openCase(String patient, String fitResult, String owner, LocalDate dueDate, Instant now) {
        policy.validateOpening(owner, dueDate);
        Task task = new Task();
        task.setStatus(Task.TaskStatus.INPROGRESS);
        task.setIntent(Task.TaskIntent.ORDER);
        task.setPriority(Task.TaskPriority.ROUTINE);
        task.setCode(new CodeableConcept(
                new Coding(CaseCodes.TASK_TYPE_SYSTEM, CaseCodes.SCREENING_CASE, "Screening case")));
        task.setFor(new Reference(patient));
        task.setFocus(new Reference(fitResult));
        task.setAuthoredOn(Date.from(now));
        applyStage(task, CaseStage.RESULT_ISSUED, owner, dueDate, now);
        return task;
    }

    public Bundle advance(Task current, TransitionRequest request, String agent, Instant now) {
        policy.validate(stageOf(current), request);
        String taskReference = current.getIdElement().toUnqualifiedVersionless().getValue();
        String currentVersion = current.getMeta().getVersionId();
        Task updated = current.copy();
        applyStage(updated, request.target(), request.owner(), request.dueDate(), now);
        applyClosure(updated, request);
        Provenance provenance = buildProvenance(taskReference, nextVersion(currentVersion), request, agent, now);
        String provenanceUrl = "urn:uuid:" + UUID.randomUUID();
        updated.addRelevantHistory(new Reference(provenanceUrl));
        return transaction(updated, taskReference, currentVersion, provenance, provenanceUrl);
    }

    private CaseStage stageOf(Task task) {
        return CaseStage.fromCode(task.getBusinessStatus().getCodingFirstRep().getCode());
    }

    private String nextVersion(String currentVersion) {
        return String.valueOf(Long.parseLong(currentVersion) + 1);
    }

    private void applyStage(Task task, CaseStage stage, String owner, LocalDate dueDate, Instant now) {
        task.setBusinessStatus(new CodeableConcept(new Coding(CaseCodes.STAGE_SYSTEM, stage.code(), null)));
        task.setOwner(new Reference(owner));
        if (dueDate != null) {
            task.getRestriction().getPeriod().setEndElement(new DateTimeType(dueDate.toString()));
        }
        task.setLastModified(Date.from(now));
    }

    private void applyClosure(Task task, TransitionRequest request) {
        if (request.target() == CaseStage.CLOSED) {
            task.setStatus(Task.TaskStatus.COMPLETED);
            request.evidence().forEach(evidence -> addEvidence(task, evidence));
        } else if (request.target() == CaseStage.CLOSED_WITHOUT_CARE) {
            task.setStatus(Task.TaskStatus.CANCELLED);
            CodeableConcept reason = new CodeableConcept(
                    new Coding(CaseCodes.CLOSURE_REASON_SYSTEM, request.reason().code(), null));
            task.setStatusReason(reason.setText(CLOSED_WITHOUT_CARE_REASON));
        }
    }

    private void addEvidence(Task task, String evidence) {
        Task.TaskOutputComponent output = task.addOutput();
        output.setType(new CodeableConcept(new Coding(CaseCodes.OUTPUT_TYPE_SYSTEM, CaseCodes.EVIDENCE, null)));
        output.setValue(new Reference(evidence));
    }

    private Provenance buildProvenance(
            String taskReference, String version, TransitionRequest request, String agent, Instant now) {
        Provenance provenance = new Provenance();
        provenance.addTarget(new Reference(taskReference + "/_history/" + version));
        provenance.setRecorded(Date.from(now));
        provenance.setActivity(new CodeableConcept(new Coding(CaseCodes.STAGE_SYSTEM, request.target().code(), null)));
        provenance.addAgent().setWho(new Reference(agent));
        if (request.humanDecisionBy() != null && !request.humanDecisionBy().isBlank()) {
            provenance.addAgent().setWho(new Reference(request.humanDecisionBy()));
        }
        return provenance;
    }

    private Bundle transaction(
            Task updated, String taskReference, String currentVersion, Provenance provenance, String provenanceUrl) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        Bundle.BundleEntryComponent taskEntry = bundle.addEntry().setResource(updated);
        taskEntry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(taskReference)
                .setIfMatch("W/\"" + currentVersion + "\"");
        Bundle.BundleEntryComponent provenanceEntry = bundle.addEntry().setResource(provenance);
        provenanceEntry.setFullUrl(provenanceUrl);
        provenanceEntry.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Provenance");
        return bundle;
    }
}
