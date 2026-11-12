package io.github.screeningloop.cases;

import java.time.LocalDate;
import java.util.List;

public record TransitionRequest(
        CaseStage target, String owner, LocalDate dueDate, List<String> evidence, String humanDecisionBy) {

    public static TransitionRequest advance(CaseStage target, String owner, LocalDate dueDate) {
        return new TransitionRequest(target, owner, dueDate, List.of(), null);
    }

    public static TransitionRequest close(String owner, List<String> evidence) {
        return new TransitionRequest(CaseStage.CLOSED, owner, null, evidence, null);
    }

    public static TransitionRequest closeWithoutCare(String owner, String humanDecisionBy) {
        return new TransitionRequest(CaseStage.CLOSED_WITHOUT_CARE, owner, null, List.of(), humanDecisionBy);
    }
}
