package io.github.screeningloop.chat;

import io.github.screeningloop.cases.CaseStage;
import java.util.Map;

public final class ReplyTemplates {

    public static final String GREETING =
            "Hello. I can tell you about your screening case, your active medications and your latest exams.";
    public static final String HELP =
            "You can ask me about the status of your screening case, your active medications or your latest exams.";
    public static final String REFUSAL =
            "I can only help with the status of your screening case, your active medications and your latest "
                    + "exams. For any medical question, please contact your care team.";
    public static final String NO_CASE = "I do not see a screening case on your record.";
    public static final String NO_MEDICATIONS = "I do not see any active medications on your record.";
    public static final String NO_EXAMS = "I do not see any recent exams on your record.";
    public static final String MEDICATIONS =
            "Your active medications on record are: {{MEDICATION_LIST}}. "
                    + "For any question about them, please ask your care team.";
    public static final String FIT_POSITIVE =
            "Your latest stool test (FIT) was done on {{FIT_DATE}} and the result was positive. "
                    + "A positive result is not a diagnosis; it means a follow-up check is recommended.";
    public static final String FIT_NEGATIVE =
            "Your latest stool test (FIT) was done on {{FIT_DATE}} and the result was negative.";
    public static final String FIT_UNKNOWN =
            "Your latest stool test (FIT) was done on {{FIT_DATE}}. Please ask your care team about the result.";
    public static final String OVERDUE =
            "Your screening case is at the stage \"{{STAGE_LABEL}}\". The next step was planned for "
                    + "{{NEXT_STEP_DATE}}, so it is taking longer than planned. "
                    + "If you would like an update, please contact your care team.";
    public static final String PROCEDURES = "Recent procedures on your record: {{PROCEDURE_LIST}}.";

    private static final String RESULT_ISSUED =
            "Your stool test (FIT) result is back and it was positive. A positive result is not a diagnosis; "
                    + "it means a follow-up check is recommended. Your care team is preparing the next step "
                    + "and aims to contact you by {{NEXT_STEP_DATE}}.";
    private static final String PATIENT_NOTIFIED =
            "Your care team has told you about your positive stool test (FIT). A positive result is not a "
                    + "diagnosis. They are waiting for you to confirm that you received the information, "
                    + "and expect that by {{NEXT_STEP_DATE}}.";
    private static final String ACKNOWLEDGED =
            "Thank you for confirming that you received your result. Your care team is arranging a "
                    + "confirmation test, planned by {{NEXT_STEP_DATE}}.";
    private static final String CONFIRMATION_DONE =
            "Your confirmation test has been done. Your care team is reviewing it and will decide the "
                    + "next step by {{NEXT_STEP_DATE}}.";
    private static final String REFERRAL_REQUESTED =
            "Your care team has asked a specialist service to see you. They are waiting for the service "
                    + "to answer, expected by {{NEXT_STEP_DATE}}.";
    private static final String REFERRAL_REJECTED =
            "The specialist service could not take the first referral. This can happen. Your care team "
                    + "is arranging another one, aiming for {{NEXT_STEP_DATE}}.";
    private static final String REFERRAL_ACCEPTED =
            "A specialist service has accepted your referral. Your care team is now arranging your "
                    + "appointment, aiming for {{NEXT_STEP_DATE}}.";
    private static final String APPOINTMENT_BOOKED =
            "Your appointment is booked. The next step on your record is dated {{NEXT_STEP_DATE}}. "
                    + "If you need to change it, please contact your care team.";
    private static final String TREATMENT_OR_SURVEILLANCE =
            "You are now in the follow-up stage with the specialist service. Your next review on record "
                    + "is dated {{NEXT_STEP_DATE}}.";
    private static final String CLOSED =
            "Your screening follow-up is complete and your care team has documented it.";
    private static final String CLOSED_WITHOUT_CARE =
            "Your screening follow-up was closed after a decision recorded by your care team. "
                    + "If you would like to continue, please contact them.";

    private static final Map<CaseStage, String> BY_STAGE = Map.ofEntries(
            Map.entry(CaseStage.RESULT_ISSUED, RESULT_ISSUED),
            Map.entry(CaseStage.PATIENT_NOTIFIED, PATIENT_NOTIFIED),
            Map.entry(CaseStage.ACKNOWLEDGED, ACKNOWLEDGED),
            Map.entry(CaseStage.CONFIRMATION_DONE, CONFIRMATION_DONE),
            Map.entry(CaseStage.REFERRAL_REQUESTED, REFERRAL_REQUESTED),
            Map.entry(CaseStage.REFERRAL_REJECTED, REFERRAL_REJECTED),
            Map.entry(CaseStage.REFERRAL_ACCEPTED, REFERRAL_ACCEPTED),
            Map.entry(CaseStage.APPOINTMENT_BOOKED, APPOINTMENT_BOOKED),
            Map.entry(CaseStage.TREATMENT_OR_SURVEILLANCE, TREATMENT_OR_SURVEILLANCE),
            Map.entry(CaseStage.CLOSED, CLOSED),
            Map.entry(CaseStage.CLOSED_WITHOUT_CARE, CLOSED_WITHOUT_CARE));

    private ReplyTemplates() {
    }

    public static String forStage(CaseStage stage) {
        String template = BY_STAGE.get(stage);
        if (template == null) {
            throw new IllegalStateException("No reply template for stage " + stage.code());
        }
        return template;
    }
}
