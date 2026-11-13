package io.github.screeningloop.chat;

import java.util.Arrays;
import java.util.List;

public enum ClinicianIntent {
    UNSUPPORTED(false, "should ", "diagnos", "prognos", "prescribe ", "recommend", "change ", "update ",
            "close ", "advance ", "delete ", "assign ", "set ", "escalate "),
    MEDICATIONS(true, "medic", "prescription", "drug", "receta"),
    FIT_RESULTS(true, "fit", "stool", "lab", "result", "test"),
    CONDITIONS(true, "condition", "problem", "comorbid"),
    PROCEDURES(true, "procedure", "colonoscop", "biops"),
    CASES(true, "case", "status", "stage", "referral", "owner", "overdue", "due "),
    SUMMARY(true, "summar", "overview", "who is", "patient"),
    HELP(false, "help", "what can", "options");

    private final boolean needsPatientData;
    private final List<String> keywords;

    ClinicianIntent(boolean needsPatientData, String... keywords) {
        this.needsPatientData = needsPatientData;
        this.keywords = List.of(keywords);
    }

    public boolean needsPatientData() {
        return needsPatientData;
    }

    public static ClinicianIntent classify(String text) {
        if (text == null || text.isBlank()) {
            return UNSUPPORTED;
        }
        String normalized = TextNormalizer.normalize(text);
        return Arrays.stream(values())
                .filter(intent -> intent.matches(normalized))
                .findFirst()
                .orElse(UNSUPPORTED);
    }

    private boolean matches(String normalized) {
        return keywords.stream().anyMatch(keyword -> normalized.contains(" " + keyword));
    }
}
