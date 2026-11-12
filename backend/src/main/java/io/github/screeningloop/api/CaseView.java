package io.github.screeningloop.api;

import org.hl7.fhir.r4.model.Task;

public record CaseView(
        String id,
        String version,
        String patient,
        String fitResult,
        String stage,
        String status,
        String owner,
        String dueDate) {

    public static CaseView from(Task task) {
        return new CaseView(
                task.getIdElement().getIdPart(),
                task.getMeta().getVersionId(),
                task.getFor().getReference(),
                task.getFocus().getReference(),
                task.getBusinessStatus().getCodingFirstRep().getCode(),
                task.getStatus().toCode(),
                task.getOwner().getReference(),
                task.getRestriction().getPeriod().getEndElement().getValueAsString());
    }
}
