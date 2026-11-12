package io.github.screeningloop.seed;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import io.github.screeningloop.cases.TransitionRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DemoCasePlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final List<String> OWNERS = List.of("Practitioner/gp-1", "Practitioner/gp-2", "Practitioner/gs-1");
    private static final int POSITIVE_PATIENTS = 33;
    private static final String EVIDENCE = "Procedure/colonoscopy-1";

    private final DemoCasePlanner planner = new DemoCasePlanner(OWNERS);
    private final CaseTransitionPolicy policy = new CaseTransitionPolicy();

    private List<DemoCaseInput> inputs(int count, String evidence) {
        return IntStream.range(0, count)
                .mapToObj(index -> new DemoCaseInput("Patient/p" + index, "Observation/fit" + index, evidence))
                .toList();
    }

    private CaseStage finalStage(DemoCasePlan plan) {
        List<TransitionRequest> steps = plan.steps();
        return steps.isEmpty() ? CaseStage.RESULT_ISSUED : steps.get(steps.size() - 1).target();
    }

    @Test
    void plansOneCasePerInputDeterministically() {
        List<DemoCaseInput> input = inputs(POSITIVE_PATIENTS, EVIDENCE);
        assertThat(planner.plan(input, TODAY)).hasSize(POSITIVE_PATIENTS)
                .isEqualTo(planner.plan(input, TODAY));
    }

    @Test
    void everyPlannedStepIsAllowedByTheTransitionPolicy() {
        for (DemoCasePlan plan : planner.plan(inputs(POSITIVE_PATIENTS, EVIDENCE), TODAY)) {
            CaseStage current = CaseStage.RESULT_ISSUED;
            for (TransitionRequest step : plan.steps()) {
                policy.validate(current, step);
                current = step.target();
            }
        }
    }

    @Test
    void coversEveryStageOutcome() {
        Set<CaseStage> reached = planner.plan(inputs(POSITIVE_PATIENTS, EVIDENCE), TODAY).stream()
                .map(this::finalStage)
                .collect(Collectors.toSet());
        assertThat(reached).containsExactlyInAnyOrder(CaseStage.values());
    }

    @Test
    void includesOverdueAndOnTimeOpenCases() {
        List<DemoCasePlan> plans = planner.plan(inputs(POSITIVE_PATIENTS, EVIDENCE), TODAY);
        List<LocalDate> finalDueDates = plans.stream()
                .filter(plan -> !finalStage(plan).isTerminal())
                .map(this::finalDueDate)
                .toList();
        assertThat(finalDueDates).anyMatch(due -> due.isBefore(TODAY)).anyMatch(due -> !due.isBefore(TODAY));
    }

    @Test
    void neverClosesWithoutEvidence() {
        List<DemoCasePlan> plans = planner.plan(inputs(POSITIVE_PATIENTS, null), TODAY);
        assertThat(plans).noneMatch(plan -> finalStage(plan) == CaseStage.CLOSED);
    }

    @Test
    void recordsAHumanDecisionWhenClosingWithoutCare() {
        List<DemoCasePlan> plans = planner.plan(inputs(POSITIVE_PATIENTS, EVIDENCE), TODAY);
        assertThat(plans).filteredOn(plan -> finalStage(plan) == CaseStage.CLOSED_WITHOUT_CARE)
                .isNotEmpty()
                .allSatisfy(plan -> assertThat(plan.steps().get(plan.steps().size() - 1).humanDecisionBy())
                        .isIn(OWNERS));
    }

    private LocalDate finalDueDate(DemoCasePlan plan) {
        List<TransitionRequest> steps = plan.steps();
        return steps.isEmpty() ? plan.dueDate() : steps.get(steps.size() - 1).dueDate();
    }
}
