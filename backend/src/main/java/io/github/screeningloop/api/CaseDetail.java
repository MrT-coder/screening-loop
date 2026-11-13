package io.github.screeningloop.api;

import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import java.util.Comparator;
import java.util.List;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Task;

public record CaseDetail(CaseView summary, List<HistoryEntry> history, List<String> allowedTargets) {

    private static final CaseTransitionPolicy POLICY = new CaseTransitionPolicy();

    public static CaseDetail from(Task task, List<Provenance> provenances) {
        List<HistoryEntry> history = provenances.stream()
                .sorted(Comparator.comparing(Provenance::getRecorded))
                .map(HistoryEntry::from)
                .toList();
        CaseView summary = CaseView.from(task);
        List<String> targets = POLICY.allowedTargets(CaseStage.fromCode(summary.stage())).stream()
                .map(CaseStage::code)
                .sorted()
                .toList();
        return new CaseDetail(summary, history, targets);
    }
}
