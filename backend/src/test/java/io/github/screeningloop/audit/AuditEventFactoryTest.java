package io.github.screeningloop.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.hl7.fhir.r4.model.AuditEvent;
import org.junit.jupiter.api.Test;

class AuditEventFactoryTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final String AGENT = "Practitioner/gp-1";
    private static final String QUERY = "Task?status=in-progress";

    private final AuditEventFactory factory = new AuditEventFactory();

    @Test
    void recordsAQueryAsAnExecuteAction() {
        AuditEvent event = factory.forQuery(AGENT, QUERY, NOW);
        assertThat(event.getAction()).isEqualTo(AuditEvent.AuditEventAction.E);
        assertThat(event.getType().getCode()).isEqualTo("rest");
        assertThat(event.getOutcome()).isEqualTo(AuditEvent.AuditEventOutcome._0);
        assertThat(event.getRecorded()).isEqualTo(Date.from(NOW));
    }

    @Test
    void namesTheRequestingAgentAndTheSource() {
        AuditEvent event = factory.forQuery(AGENT, QUERY, NOW);
        assertThat(event.getAgentFirstRep().getWho().getReference()).isEqualTo(AGENT);
        assertThat(event.getAgentFirstRep().getRequestor()).isTrue();
        assertThat(event.getSource().getObserver().getDisplay()).isEqualTo("screening-loop");
    }

    @Test
    void keepsTheSearchInTheEntityQuery() {
        AuditEvent event = factory.forQuery(AGENT, QUERY, NOW);
        byte[] stored = event.getEntityFirstRep().getQuery();
        assertThat(new String(stored, StandardCharsets.UTF_8)).isEqualTo(QUERY);
    }
}
