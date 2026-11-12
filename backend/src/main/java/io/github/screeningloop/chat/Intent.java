package io.github.screeningloop.chat;

public enum Intent {
    GREETING(false),
    HELP(false),
    CASE_STATUS(true),
    MEDICATIONS(true),
    LATEST_EXAMS(true),
    UNSUPPORTED(false);

    private final boolean needsPatientData;

    Intent(boolean needsPatientData) {
        this.needsPatientData = needsPatientData;
    }

    public boolean needsPatientData() {
        return needsPatientData;
    }
}
