package io.github.screeningloop.chat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.patient.PatientSummary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class ClinicianReplies {

    static final String HELP = "I can show recorded data for this patient: summary, FIT results, conditions, "
            + "medications, procedures and cases.";
    static final String READ_ONLY = "This chat is read-only and shows recorded data only. It gives no clinical "
            + "advice. To advance or escalate a case, use the form below.";

    private static final int ISO_DATE_LENGTH = 10;

    private static final Map<ClinicianIntent, Function<PatientSummary, String>> DATA_REPLIES = Map.of(
            ClinicianIntent.SUMMARY, ClinicianReplies::summarize,
            ClinicianIntent.FIT_RESULTS, summary -> list("FIT results", fitLines(summary),
                    "No FIT results on record."),
            ClinicianIntent.CONDITIONS, summary -> list("Conditions", conditionLines(summary),
                    "No conditions on record."),
            ClinicianIntent.MEDICATIONS, summary -> list("Active medications", summary.activeMedications(),
                    "No active medications on record."),
            ClinicianIntent.PROCEDURES, summary -> list("Recent procedures", procedureLines(summary),
                    "No recent procedures on record."),
            ClinicianIntent.CASES, summary -> list("Cases", caseLines(summary), "No cases on record."));

    private ClinicianReplies() {
    }

    static String compose(ClinicianIntent intent, PatientSummary summary) {
        if (intent.needsPatientData()) {
            return DATA_REPLIES.get(intent).apply(summary);
        }
        return intent == ClinicianIntent.HELP ? HELP : READ_ONLY;
    }

    private static String summarize(PatientSummary summary) {
        String age = summary.age() == null ? "age unknown" : summary.age() + " years";
        String latestFit = summary.fitResults().isEmpty() ? "none"
                : summary.fitResults().get(0).interpretation();
        return String.format("%s, %s, %s. Latest FIT: %s. Active medications: %d. Conditions on record: %d. "
                        + "Cases: %d.",
                summary.name(), summary.gender(), age, latestFit, summary.activeMedications().size(),
                summary.conditions().size(), summary.cases().size());
    }

    private static String list(String title, List<String> lines, String emptyText) {
        return lines.isEmpty() ? emptyText : title + ": " + String.join("; ", lines) + ".";
    }

    private static List<String> fitLines(PatientSummary summary) {
        return summary.fitResults().stream()
                .map(fit -> String.format("%s %s %s (%s)", date(fit.date()), fit.value(), fit.unit(),
                        fit.interpretation()))
                .toList();
    }

    private static List<String> conditionLines(PatientSummary summary) {
        return summary.conditions().stream()
                .map(condition -> condition.name() + " (" + condition.status() + ")")
                .toList();
    }

    private static List<String> procedureLines(PatientSummary summary) {
        return summary.recentProcedures().stream()
                .map(procedure -> date(procedure.date()) + ": " + procedure.name())
                .toList();
    }

    private static List<String> caseLines(PatientSummary summary) {
        return summary.cases().stream().map(ClinicianReplies::caseLine).toList();
    }

    private static String caseLine(CaseView view) {
        return String.format("%s is at %s, owner %s, due %s", view.id(), view.stage(), shortId(view.owner()),
                view.dueDate() == null ? "not set" : view.dueDate());
    }

    private static String shortId(String reference) {
        return reference == null ? "unassigned" : reference.substring(reference.lastIndexOf('/') + 1);
    }

    private static String date(String isoDateOrDateTime) {
        return isoDateOrDateTime.length() > ISO_DATE_LENGTH
                ? isoDateOrDateTime.substring(0, ISO_DATE_LENGTH) : isoDateOrDateTime;
    }
}
