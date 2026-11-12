package io.github.screeningloop.seed;

import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.cases.TransitionRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class DemoCasePlanner {

    private static final int OVERDUE_DAYS = 6;
    private static final int ON_TIME_DAYS = 9;
    private static final int PAST_STEP_DAYS = 40;

    private static final List<CaseStage> MAIN_PATH = List.of(
            CaseStage.RESULT_ISSUED,
            CaseStage.PATIENT_NOTIFIED,
            CaseStage.ACKNOWLEDGED,
            CaseStage.CONFIRMATION_DONE,
            CaseStage.REFERRAL_REQUESTED,
            CaseStage.REFERRAL_ACCEPTED,
            CaseStage.APPOINTMENT_BOOKED,
            CaseStage.TREATMENT_OR_SURVEILLANCE);

    private static final List<Outcome> OUTCOMES = List.of(
            new Outcome(CaseStage.RESULT_ISSUED, true),
            new Outcome(CaseStage.PATIENT_NOTIFIED, false),
            new Outcome(CaseStage.ACKNOWLEDGED, true),
            new Outcome(CaseStage.CONFIRMATION_DONE, false),
            new Outcome(CaseStage.REFERRAL_REQUESTED, true),
            new Outcome(CaseStage.REFERRAL_REJECTED, false),
            new Outcome(CaseStage.REFERRAL_ACCEPTED, false),
            new Outcome(CaseStage.APPOINTMENT_BOOKED, true),
            new Outcome(CaseStage.TREATMENT_OR_SURVEILLANCE, false),
            new Outcome(CaseStage.CLOSED, false),
            new Outcome(CaseStage.CLOSED_WITHOUT_CARE, false),
            new Outcome(CaseStage.RESULT_ISSUED, false));

    private final List<String> owners;

    public DemoCasePlanner(List<String> owners) {
        this.owners = List.copyOf(owners);
    }

    public List<DemoCasePlan> plan(List<DemoCaseInput> inputs, LocalDate today) {
        return IntStream.range(0, inputs.size())
                .mapToObj(index -> planOne(index, inputs.get(index), today))
                .toList();
    }

    private DemoCasePlan planOne(int index, DemoCaseInput input, LocalDate today) {
        Outcome outcome = OUTCOMES.get(index % OUTCOMES.size());
        List<CaseStage> path = pathTo(outcome.finalStage(), input.evidence() != null);
        LocalDate finalDue = today.plusDays(outcome.isOverdue() ? -OVERDUE_DAYS : ON_TIME_DAYS);
        List<TransitionRequest> steps = new ArrayList<>();
        for (int position = 1; position < path.size(); position++) {
            boolean isLast = position == path.size() - 1;
            LocalDate due = isLast ? finalDue : today.minusDays(PAST_STEP_DAYS - position);
            steps.add(stepTo(path.get(position), ownerAt(index, position), due, input));
        }
        LocalDate openingDue = steps.isEmpty() ? finalDue : today.minusDays(PAST_STEP_DAYS);
        return new DemoCasePlan(input.patient(), input.fitResult(), ownerAt(index, 0), openingDue, steps);
    }

    private List<CaseStage> pathTo(CaseStage target, boolean hasEvidence) {
        CaseStage effective = target == CaseStage.CLOSED && !hasEvidence
                ? CaseStage.TREATMENT_OR_SURVEILLANCE
                : target;
        return switch (effective) {
            case REFERRAL_REJECTED -> append(upTo(CaseStage.REFERRAL_REQUESTED), CaseStage.REFERRAL_REJECTED);
            case CLOSED -> append(MAIN_PATH, CaseStage.CLOSED);
            case CLOSED_WITHOUT_CARE -> append(upTo(CaseStage.PATIENT_NOTIFIED), CaseStage.CLOSED_WITHOUT_CARE);
            default -> upTo(effective);
        };
    }

    private List<CaseStage> upTo(CaseStage stage) {
        return MAIN_PATH.subList(0, MAIN_PATH.indexOf(stage) + 1);
    }

    private List<CaseStage> append(List<CaseStage> path, CaseStage stage) {
        List<CaseStage> extended = new ArrayList<>(path);
        extended.add(stage);
        return extended;
    }

    private TransitionRequest stepTo(CaseStage target, String owner, LocalDate due, DemoCaseInput input) {
        return switch (target) {
            case CLOSED -> TransitionRequest.close(owner, List.of(input.evidence()));
            case CLOSED_WITHOUT_CARE -> TransitionRequest.closeWithoutCare(owner, owners.get(0));
            default -> TransitionRequest.advance(target, owner, due);
        };
    }

    private String ownerAt(int index, int position) {
        return owners.get((index + position) % owners.size());
    }

    private record Outcome(CaseStage finalStage, boolean isOverdue) {
    }
}
