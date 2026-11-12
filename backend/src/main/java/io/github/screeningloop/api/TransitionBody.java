package io.github.screeningloop.api;

import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.cases.TransitionRequest;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

public record TransitionBody(
        @NotBlank String target, String owner, LocalDate dueDate, List<String> evidence, String humanDecisionBy) {

    public TransitionRequest toRequest() {
        List<String> references = evidence == null ? List.of() : evidence;
        return new TransitionRequest(CaseStage.fromCode(target), owner, dueDate, references, humanDecisionBy);
    }
}
