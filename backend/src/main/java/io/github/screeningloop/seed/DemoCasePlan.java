package io.github.screeningloop.seed;

import io.github.screeningloop.cases.TransitionRequest;
import java.time.LocalDate;
import java.util.List;

public record DemoCasePlan(
        String patient, String fitResult, String owner, LocalDate dueDate, List<TransitionRequest> steps) {
}
