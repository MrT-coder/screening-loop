package io.github.screeningloop.cases;

import java.time.LocalDate;
import java.util.List;

public record TransitionRequest(
        CaseStage target,
        String owner,
        LocalDate dueDate,
        List<String> evidence,
        String humanDecisionBy,
        ClosureReason reason) {

    public TransitionRequest withHumanDecisionBy(String decider) {
        return new TransitionRequest(target, owner, dueDate, evidence, decider, reason);
    }

    public static TransitionRequest advance(CaseStage target, String owner, LocalDate dueDate) {
        return new TransitionRequest(target, owner, dueDate, List.of(), null, null);
    }

    public static TransitionRequest close(String owner, List<String> evidence) {
        return new TransitionRequest(CaseStage.CLOSED, owner, null, evidence, null, null);
    }

    public static TransitionRequest closeWithoutCare(String owner, String humanDecisionBy, ClosureReason reason) {
        return new TransitionRequest(
                CaseStage.CLOSED_WITHOUT_CARE, owner, null, List.of(), humanDecisionBy, reason);
    }
}
