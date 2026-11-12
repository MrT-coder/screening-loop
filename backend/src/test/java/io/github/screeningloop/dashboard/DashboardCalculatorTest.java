package io.github.screeningloop.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.cases.CaseTaskMapper;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;

class DashboardCalculatorTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final LocalDate PAST = LocalDate.parse("2026-11-01");
    private static final LocalDate FUTURE = LocalDate.parse("2026-12-01");
    private static final Instant OPENED_AT = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-11-11T00:00:00Z");
    private static final double TEN_DAYS = 10.0;
    private static final int TOTAL_CASES = 4;
    private static final String OWNER = "Practitioner/gp-1";

    private final CaseTaskMapper mapper = new CaseTaskMapper(new CaseTransitionPolicy());
    private final DashboardCalculator calculator = new DashboardCalculator();

    @Test
    void flagsOnlyOpenCasesWithAPastDueDateAsOverdue() {
        Task overdue = task("1", CaseStage.PATIENT_NOTIFIED, PAST);
        Task dueToday = task("2", CaseStage.PATIENT_NOTIFIED, TODAY);
        Task future = task("3", CaseStage.PATIENT_NOTIFIED, FUTURE);
        Task closed = closed("4", CaseStage.CLOSED);
        closed.getRestriction().getPeriod().setEndElement(new DateTimeType(PAST.toString()));
        DashboardSummary summary = calculator.summarize(List.of(overdue, dueToday, future, closed), TODAY);
        assertThat(summary.overdue()).extracting(CaseView::id).containsExactly("1");
    }

    @Test
    void flagsOpenCasesWithoutOwnerAsUnassigned() {
        Task unassigned = task("1", CaseStage.ACKNOWLEDGED, FUTURE);
        unassigned.setOwner(null);
        Task assigned = task("2", CaseStage.ACKNOWLEDGED, FUTURE);
        DashboardSummary summary = calculator.summarize(List.of(unassigned, assigned), TODAY);
        assertThat(summary.unassigned()).extracting(CaseView::id).containsExactly("1");
    }

    @Test
    void flagsCasesWhoseReferralWasRejected() {
        Task rejected = task("1", CaseStage.REFERRAL_REJECTED, FUTURE);
        Task requested = task("2", CaseStage.REFERRAL_REQUESTED, FUTURE);
        DashboardSummary summary = calculator.summarize(List.of(rejected, requested), TODAY);
        assertThat(summary.rejected()).extracting(CaseView::id).containsExactly("1");
    }

    @Test
    void countsCasesByOutcomeAndStage() {
        List<Task> tasks = List.of(
                task("1", CaseStage.RESULT_ISSUED, FUTURE),
                task("2", CaseStage.RESULT_ISSUED, FUTURE),
                closed("3", CaseStage.CLOSED),
                closed("4", CaseStage.CLOSED_WITHOUT_CARE));
        DashboardSummary summary = calculator.summarize(tasks, TODAY);
        assertThat(summary.total()).isEqualTo(TOTAL_CASES);
        assertThat(summary.open()).isEqualTo(2);
        assertThat(summary.closed()).isEqualTo(1);
        assertThat(summary.closedWithoutCare()).isEqualTo(1);
        assertThat(summary.byStage()).containsEntry("result-issued", 2).containsEntry("closed", 1)
                .containsEntry("acknowledged", 0);
    }

    @Test
    void averagesTheDaysItTookToCloseCases() {
        DashboardSummary summary = calculator.summarize(List.of(closed("1", CaseStage.CLOSED)), TODAY);
        assertThat(summary.averageDaysToClose()).isEqualTo(TEN_DAYS);
    }

    @Test
    void hasNoAverageWhenNothingIsClosed() {
        DashboardSummary summary = calculator.summarize(List.of(task("1", CaseStage.RESULT_ISSUED, FUTURE)), TODAY);
        assertThat(summary.averageDaysToClose()).isNull();
    }

    private Task task(String id, CaseStage stage, LocalDate dueDate) {
        Task task = mapper.openCase("Patient/p1", "Observation/fit-1", OWNER, dueDate, OPENED_AT);
        task.setId("Task/" + id);
        task.getMeta().setVersionId("1");
        task.getBusinessStatus().getCodingFirstRep().setCode(stage.code());
        return task;
    }

    private Task closed(String id, CaseStage stage) {
        Task task = task(id, stage, FUTURE);
        task.setStatus(stage == CaseStage.CLOSED ? Task.TaskStatus.COMPLETED : Task.TaskStatus.CANCELLED);
        task.setLastModified(Date.from(CLOSED_AT));
        return task;
    }
}
