package io.github.screeningloop.dashboard;

import io.github.screeningloop.api.CaseView;
import java.util.List;
import java.util.Map;

public record DashboardSummary(
        int total,
        int open,
        int closed,
        int closedWithoutCare,
        Map<String, Integer> byStage,
        List<CaseView> overdue,
        List<CaseView> unassigned,
        List<CaseView> rejected,
        Double averageDaysToClose) {
}
