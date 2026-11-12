package io.github.screeningloop.cases;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.github.screeningloop.audit.AuditEventFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Service;

@Service
public class CaseService {

    private static final String OPEN_CASES_QUERY = "Task?status=in-progress";
    private static final int PAGE_SIZE = 200;

    private final IGenericClient client;
    private final Clock clock;
    private final CaseTaskMapper mapper = new CaseTaskMapper(new CaseTransitionPolicy());
    private final AuditEventFactory auditEvents = new AuditEventFactory();

    public CaseService(IGenericClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public Task openCase(String patient, String fitResult, String owner, LocalDate dueDate) {
        Task task = mapper.openCase(patient, fitResult, owner, dueDate, clock.instant());
        String id = client.create().resource(task).execute().getId().getIdPart();
        return readTask(id);
    }

    public Task advance(String taskId, TransitionRequest request, String agent) {
        Task current = readTask(taskId);
        Bundle transaction = mapper.advance(current, request, agent, clock.instant());
        client.transaction().withBundle(transaction).execute();
        return readTask(taskId);
    }

    public List<Task> findOpenCases(String agent) {
        Bundle bundle = client.search()
                .forResource(Task.class)
                .where(Task.STATUS.exactly().code(Task.TaskStatus.INPROGRESS.toCode()))
                .count(PAGE_SIZE)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        client.create().resource(auditEvents.forQuery(agent, OPEN_CASES_QUERY, clock.instant())).execute();
        return bundle.getEntry().stream()
                .map(entry -> (Task) entry.getResource())
                .toList();
    }

    private Task readTask(String id) {
        return client.read().resource(Task.class).withId(id).execute();
    }
}
