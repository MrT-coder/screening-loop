package io.github.screeningloop.cases;

public enum CaseStage {
    RESULT_ISSUED("result-issued", false),
    PATIENT_NOTIFIED("patient-notified", false),
    ACKNOWLEDGED("acknowledged", false),
    CONFIRMATION_DONE("confirmation-done", false),
    REFERRAL_REQUESTED("referral-requested", false),
    REFERRAL_REJECTED("referral-rejected", false),
    REFERRAL_ACCEPTED("referral-accepted", false),
    APPOINTMENT_BOOKED("appointment-booked", false),
    TREATMENT_OR_SURVEILLANCE("treatment-or-surveillance", false),
    CLOSED("closed", true),
    CLOSED_WITHOUT_CARE("closed-without-care", true);

    private final String code;
    private final boolean isTerminal;

    CaseStage(String code, boolean isTerminal) {
        this.code = code;
        this.isTerminal = isTerminal;
    }

    public String code() {
        return code;
    }

    public boolean isTerminal() {
        return isTerminal;
    }
}
