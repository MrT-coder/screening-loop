package io.github.screeningloop.dashboard;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.hl7.fhir.r4.model.Task;

public class DashboardCalculator {

    private static final double HOURS_PER_DAY = 24.0;

    public DashboardSummary summarize(List<Task> tasks, LocalDate today) {
        List<Task> open = tasks.stream().filter(task -> !stageOf(task).isTerminal()).toList();
        return new DashboardSummary(
                tasks.size(),
                open.size(),
                count(tasks, CaseStage.CLOSED),
                count(tasks, CaseStage.CLOSED_WITHOUT_CARE),
                countByStage(tasks),
                views(open, task -> isOverdue(task, today)),
                views(open, task -> !task.hasOwner()),
                views(open, task -> stageOf(task) == CaseStage.REFERRAL_REJECTED),
                averageDaysToClose(tasks));
    }

    private CaseStage stageOf(Task task) {
        return CaseStage.fromCode(task.getBusinessStatus().getCodingFirstRep().getCode());
    }

    private int count(List<Task> tasks, CaseStage stage) {
        return (int) tasks.stream().filter(task -> stageOf(task) == stage).count();
    }

    private Map<String, Integer> countByStage(List<Task> tasks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Arrays.stream(CaseStage.values()).forEach(stage -> counts.put(stage.code(), count(tasks, stage)));
        return counts;
    }

    private List<CaseView> views(List<Task> tasks, Predicate<Task> filter) {
        return tasks.stream().filter(filter).map(CaseView::from).toList();
    }

    private boolean isOverdue(Task task, LocalDate today) {
        if (!task.getRestriction().getPeriod().hasEnd()) {
            return false;
        }
        LocalDate due = task.getRestriction().getPeriod().getEnd().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return due.isBefore(today);
    }

    private Double averageDaysToClose(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> stageOf(task) == CaseStage.CLOSED)
                .mapToDouble(this::daysToClose)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private double daysToClose(Task task) {
        return Duration.between(task.getAuthoredOn().toInstant(), task.getLastModified().toInstant()).toHours()
                / HOURS_PER_DAY;
    }
}
