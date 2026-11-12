package io.github.screeningloop.cases;

public enum ClosureReason {
    PATIENT_DECLINED("patient-declined"),
    UNREACHABLE("unreachable"),
    RELOCATED("relocated"),
    CARE_ELSEWHERE("care-elsewhere"),
    OTHER("other");

    private final String code;

    ClosureReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ClosureReason fromCode(String code) {
        for (ClosureReason reason : values()) {
            if (reason.code.equals(code)) {
                return reason;
            }
        }
        throw new InvalidTransitionException("Unknown closure reason code: " + code);
    }
}
