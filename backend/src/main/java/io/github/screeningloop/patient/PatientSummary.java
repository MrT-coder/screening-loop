package io.github.screeningloop.patient;

import io.github.screeningloop.api.CaseView;
import java.util.List;

public record PatientSummary(
        String id,
        String name,
        String gender,
        Integer age,
        List<FitResult> fitResults,
        List<ConditionEntry> conditions,
        List<String> activeMedications,
        List<ProcedureEntry> recentProcedures,
        List<CaseView> cases) {

    public record FitResult(String date, Double value, String unit, String interpretation) {
    }

    public record ConditionEntry(String name, String status) {
    }

    public record ProcedureEntry(String name, String date) {
    }
}
