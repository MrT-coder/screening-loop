package io.github.screeningloop.audit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;

public class AuditEventFactory {

    private static final String TYPE_SYSTEM = "http://terminology.hl7.org/CodeSystem/audit-event-type";
    private static final String SOURCE_NAME = "screening-loop";

    public AuditEvent forQuery(String agent, String query, Instant now) {
        AuditEvent event = new AuditEvent();
        event.setType(new Coding(TYPE_SYSTEM, "rest", "Restful Operation"));
        event.setAction(AuditEvent.AuditEventAction.E);
        event.setRecorded(Date.from(now));
        event.setOutcome(AuditEvent.AuditEventOutcome._0);
        event.addAgent().setWho(new Reference(agent)).setRequestor(true);
        event.getSource().setObserver(new Reference().setDisplay(SOURCE_NAME));
        event.addEntity().setQuery(query.getBytes(StandardCharsets.UTF_8));
        return event;
    }
}
