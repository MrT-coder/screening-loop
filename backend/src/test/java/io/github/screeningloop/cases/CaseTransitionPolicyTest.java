package io.github.screeningloop.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseTransitionPolicyTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 1);
    private static final String OWNER = "Practitioner/gp-1";
    private static final String EVIDENCE = "DiagnosticReport/biopsy-1";
    private static final String HUMAN_DECISION_BY = "Practitioner/supervisor-1";

    private final CaseTransitionPolicy policy = new CaseTransitionPolicy();

    @Test
    void acceptsTheNextStageWithOwnerAndDueDate() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, OWNER, DUE_DATE);
        assertThatCode(() -> policy.validate(CaseStage.RESULT_ISSUED, request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSkippingAStage() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.APPOINTMENT_BOOKED, OWNER, DUE_DATE);
        assertThatThrownBy(() -> policy.validate(CaseStage.RESULT_ISSUED, request))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("result-issued")
                .hasMessageContaining("appointment-booked");
    }

    @Test
    void rejectsATransitionWithoutOwner() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, " ", DUE_DATE);
        assertThatThrownBy(() -> policy.validate(CaseStage.RESULT_ISSUED, request))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void rejectsAnOpenStageWithoutDueDate() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, OWNER, null);
        assertThatThrownBy(() -> policy.validate(CaseStage.RESULT_ISSUED, request))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("due date");
    }

    @Test
    void rejectedReferralGoesBackToReferralRequestedWithANewOwner() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.REFERRAL_REQUESTED, OWNER, DUE_DATE);
        assertThatCode(() -> policy.validate(CaseStage.REFERRAL_REJECTED, request)).doesNotThrowAnyException();
        assertThat(policy.allowedTargets(CaseStage.REFERRAL_REQUESTED))
                .contains(CaseStage.REFERRAL_ACCEPTED, CaseStage.REFERRAL_REJECTED);
    }

    @Test
    void closesWithEvidence() {
        TransitionRequest request = TransitionRequest.close(OWNER, List.of(EVIDENCE));
        assertThatCode(() -> policy.validate(CaseStage.TREATMENT_OR_SURVEILLANCE, request))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsClosureWithoutEvidence() {
        TransitionRequest request = TransitionRequest.close(OWNER, List.of());
        assertThatThrownBy(() -> policy.validate(CaseStage.TREATMENT_OR_SURVEILLANCE, request))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("evidence");
    }

    @Test
    void rejectsClosureBeforeConfirmationIsDone() {
        TransitionRequest request = TransitionRequest.close(OWNER, List.of(EVIDENCE));
        assertThatThrownBy(() -> policy.validate(CaseStage.PATIENT_NOTIFIED, request))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void closesWithoutCareOnlyWithAHumanDecision() {
        TransitionRequest withDecision = TransitionRequest.closeWithoutCare(OWNER, HUMAN_DECISION_BY);
        TransitionRequest withoutDecision = TransitionRequest.closeWithoutCare(OWNER, "");
        assertThatCode(() -> policy.validate(CaseStage.PATIENT_NOTIFIED, withDecision)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validate(CaseStage.PATIENT_NOTIFIED, withoutDecision))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("human decision");
    }

    @Test
    void rejectsAnyTransitionOutOfATerminalStage() {
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, OWNER, DUE_DATE);
        assertThatThrownBy(() -> policy.validate(CaseStage.CLOSED, request))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> policy.validate(CaseStage.CLOSED_WITHOUT_CARE, request))
                .isInstanceOf(InvalidTransitionException.class);
    }
}
