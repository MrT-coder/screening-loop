package io.github.screeningloop.api;

import java.util.Comparator;
import java.util.List;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Task;

public record CaseDetail(CaseView summary, List<HistoryEntry> history) {

    public static CaseDetail from(Task task, List<Provenance> provenances) {
        List<HistoryEntry> history = provenances.stream()
                .sorted(Comparator.comparing(Provenance::getRecorded))
                .map(HistoryEntry::from)
                .toList();
        return new CaseDetail(CaseView.from(task), history);
    }
}
