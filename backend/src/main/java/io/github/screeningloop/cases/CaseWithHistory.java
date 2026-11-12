package io.github.screeningloop.cases;

import java.util.List;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Task;

public record CaseWithHistory(Task task, List<Provenance> history) {
}
