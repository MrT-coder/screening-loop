package io.github.screeningloop.dashboard;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.github.screeningloop.audit.AuditEventFactory;
import io.github.screeningloop.cases.CaseCodes;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final String CASES_QUERY =
            "Task?code=" + CaseCodes.TASK_TYPE_SYSTEM + "|" + CaseCodes.SCREENING_CASE;
    private static final int PAGE_SIZE = 200;

    private final IGenericClient client;
    private final Clock clock;
    private final DashboardCalculator calculator = new DashboardCalculator();
    private final AuditEventFactory auditEvents = new AuditEventFactory();

    public DashboardService(IGenericClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public DashboardSummary summarize(String agent) {
        List<Task> tasks = loadAllCases();
        client.create().resource(auditEvents.forQuery(agent, CASES_QUERY, clock.instant())).execute();
        return calculator.summarize(tasks, LocalDate.now(clock));
    }

    private List<Task> loadAllCases() {
        Bundle page = client.search()
                .forResource(Task.class)
                .where(Task.CODE.exactly().systemAndCode(CaseCodes.TASK_TYPE_SYSTEM, CaseCodes.SCREENING_CASE))
                .count(PAGE_SIZE)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        List<Task> tasks = new ArrayList<>();
        collect(page, tasks);
        while (page.getLink(Bundle.LINK_NEXT) != null) {
            page = client.loadPage().next(page).execute();
            collect(page, tasks);
        }
        return tasks;
    }

    private void collect(Bundle page, List<Task> tasks) {
        page.getEntry().forEach(entry -> tasks.add((Task) entry.getResource()));
    }
}
