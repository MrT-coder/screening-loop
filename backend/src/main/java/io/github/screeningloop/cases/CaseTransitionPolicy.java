package io.github.screeningloop.cases;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class CaseTransitionPolicy {

    private static final Set<CaseStage> OPEN_STAGES = EnumSet.complementOf(
            EnumSet.of(CaseStage.CLOSED, CaseStage.CLOSED_WITHOUT_CARE));

    private static final Map<CaseStage, Set<CaseStage>> FORWARD = Map.of(
            CaseStage.RESULT_ISSUED, EnumSet.of(CaseStage.PATIENT_NOTIFIED),
            CaseStage.PATIENT_NOTIFIED, EnumSet.of(CaseStage.ACKNOWLEDGED),
            CaseStage.ACKNOWLEDGED, EnumSet.of(CaseStage.CONFIRMATION_DONE),
            CaseStage.CONFIRMATION_DONE, EnumSet.of(CaseStage.REFERRAL_REQUESTED, CaseStage.CLOSED),
            CaseStage.REFERRAL_REQUESTED, EnumSet.of(CaseStage.REFERRAL_ACCEPTED, CaseStage.REFERRAL_REJECTED),
            CaseStage.REFERRAL_REJECTED, EnumSet.of(CaseStage.REFERRAL_REQUESTED),
            CaseStage.REFERRAL_ACCEPTED, EnumSet.of(CaseStage.APPOINTMENT_BOOKED),
            CaseStage.APPOINTMENT_BOOKED, EnumSet.of(CaseStage.TREATMENT_OR_SURVEILLANCE),
            CaseStage.TREATMENT_OR_SURVEILLANCE, EnumSet.of(CaseStage.CLOSED));

    public Set<CaseStage> allowedTargets(CaseStage current) {
        if (!OPEN_STAGES.contains(current)) {
            return Set.of();
        }
        Set<CaseStage> targets = EnumSet.of(CaseStage.CLOSED_WITHOUT_CARE);
        targets.addAll(FORWARD.getOrDefault(current, Set.of()));
        return targets;
    }

    public void validate(CaseStage current, TransitionRequest request) {
        if (!allowedTargets(current).contains(request.target())) {
            throw new InvalidTransitionException(
                    "Transition from " + current.code() + " to " + request.target().code() + " is not allowed");
        }
        requireText(request.owner(), "A transition requires an owner");
        if (!request.target().isTerminal() && request.dueDate() == null) {
            throw new InvalidTransitionException("A transition to an open stage requires a due date");
        }
        validateClosure(request);
    }

    private void validateClosure(TransitionRequest request) {
        if (request.target() == CaseStage.CLOSED
                && (request.evidence() == null || request.evidence().isEmpty())) {
            throw new InvalidTransitionException("Closing a case requires at least one evidence reference");
        }
        if (request.target() == CaseStage.CLOSED_WITHOUT_CARE) {
            requireText(request.humanDecisionBy(), "Closing without care requires a recorded human decision");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransitionException(message);
        }
    }
}
