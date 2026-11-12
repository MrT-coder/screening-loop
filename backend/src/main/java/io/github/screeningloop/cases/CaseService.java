package io.github.screeningloop.cases;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.github.screeningloop.access.AccessPolicy;
import io.github.screeningloop.access.Actor;
import io.github.screeningloop.audit.AuditEventFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Provenance;
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
    private final AccessPolicy accessPolicy = new AccessPolicy();

    public CaseService(IGenericClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public Task openCase(String patient, String fitResult, String owner, LocalDate dueDate, Actor actor) {
        accessPolicy.requireCanOpen(actor);
        Task task = mapper.openCase(patient, fitResult, owner, dueDate, clock.instant());
        String id = client.create().resource(task).execute().getId().getIdPart();
        return readTask(id);
    }

    public Task advance(String taskId, TransitionRequest request, Actor actor) {
        Task current = readTask(taskId);
        accessPolicy.requireCanAdvance(actor, current.getOwner().getReference(), request.target());
        TransitionRequest decided = request.target() == CaseStage.CLOSED_WITHOUT_CARE
                ? request.withHumanDecisionBy(actor.practitioner())
                : request;
        Bundle transaction = mapper.advance(current, decided, actor.practitioner(), clock.instant());
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

    public CaseWithHistory findCase(String taskId, String agent) {
        Task task = readTask(taskId);
        List<Provenance> history = loadHistory(task);
        client.create().resource(auditEvents.forQuery(agent, "Task/" + taskId, clock.instant())).execute();
        return new CaseWithHistory(task, history);
    }

    private List<Provenance> loadHistory(Task task) {
        List<String> ids = task.getRelevantHistory().stream()
                .map(reference -> new IdType(reference.getReference()).getIdPart())
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Bundle bundle = client.search()
                .forResource(Provenance.class)
                .where(Provenance.RES_ID.exactly().codes(ids))
                .count(PAGE_SIZE)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(entry -> (Provenance) entry.getResource())
                .toList();
    }

    private Task readTask(String id) {
        return client.read().resource(Task.class).withId(id).execute();
    }
}
