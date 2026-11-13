package io.github.screeningloop.chat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.patient.PatientSummary;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

final class ClinicianReplies {

    private static final String CATALOG = "clinician";
    private static final int ISO_DATE_LENGTH = 10;
    private static final Map<Language, MessageCatalog> CATALOGS = loadAll();

    private static final Map<ClinicianIntent, BiFunction<PatientSummary, MessageCatalog, String>> DATA_REPLIES =
            Map.of(
                    ClinicianIntent.SUMMARY, ClinicianReplies::summarize,
                    ClinicianIntent.FIT_RESULTS, (summary, catalog) -> list(catalog, "fit", fitLines(summary)),
                    ClinicianIntent.CONDITIONS,
                    (summary, catalog) -> list(catalog, "conditions", conditionLines(summary)),
                    ClinicianIntent.MEDICATIONS,
                    (summary, catalog) -> list(catalog, "medications", summary.activeMedications()),
                    ClinicianIntent.PROCEDURES,
                    (summary, catalog) -> list(catalog, "procedures", procedureLines(summary)),
                    ClinicianIntent.CASES, (summary, catalog) -> list(catalog, "cases", caseLines(summary, catalog)));

    private ClinicianReplies() {
    }

    private static Map<Language, MessageCatalog> loadAll() {
        Map<Language, MessageCatalog> loaded = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            loaded.put(language, MessageCatalog.load(CATALOG, language));
        }
        return loaded;
    }

    static String compose(ClinicianIntent intent, PatientSummary summary) {
        return compose(intent, summary, Language.EN);
    }

    static String compose(ClinicianIntent intent, PatientSummary summary, Language language) {
        MessageCatalog catalog = CATALOGS.get(language);
        if (intent.needsPatientData()) {
            return DATA_REPLIES.get(intent).apply(summary, catalog);
        }
        return catalog.get(intent == ClinicianIntent.HELP ? "help" : "readOnly");
    }

    private static String summarize(PatientSummary summary, MessageCatalog catalog) {
        String age = summary.age() == null
                ? catalog.get("ageUnknown") : catalog.get("ageYears").formatted(summary.age());
        String latestFit = summary.fitResults().isEmpty() ? catalog.get("none")
                : summary.fitResults().get(0).interpretation();
        return catalog.get("summary").formatted(summary.name(), summary.gender(), age, latestFit,
                summary.activeMedications().size(), summary.conditions().size(), summary.cases().size());
    }

    private static String list(MessageCatalog catalog, String key, List<String> lines) {
        return lines.isEmpty()
                ? catalog.get(key + ".empty")
                : catalog.get(key + ".title") + ": " + String.join("; ", lines) + ".";
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

    private static List<String> caseLines(PatientSummary summary, MessageCatalog catalog) {
        return summary.cases().stream().map(view -> caseLine(view, catalog)).toList();
    }

    private static String caseLine(CaseView view, MessageCatalog catalog) {
        return catalog.get("caseLine").formatted(view.id(), view.stage(), shortId(view.owner(), catalog),
                view.dueDate() == null ? catalog.get("notSet") : view.dueDate());
    }

    private static String shortId(String reference, MessageCatalog catalog) {
        return reference == null ? catalog.get("unassigned") : reference.substring(reference.lastIndexOf('/') + 1);
    }

    private static String date(String isoDateOrDateTime) {
        return isoDateOrDateTime.length() > ISO_DATE_LENGTH
                ? isoDateOrDateTime.substring(0, ISO_DATE_LENGTH) : isoDateOrDateTime;
    }
}
