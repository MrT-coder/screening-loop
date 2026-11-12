package io.github.screeningloop.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatientSummaryServiceIntegrationTest {

    private static final String SERVER_URL = "http://127.0.0.1:8090/fhir";

    private IGenericClient client;
    private PatientSummaryService service;
    private String patientId;
    private String agent;
    private Set<String> auditIdsBefore;

    @BeforeEach
    void connect() {
        client = FhirContext.forR4Cached().newRestfulGenericClient(SERVER_URL);
        assumeTrue(isReachable(), "HAPI server is not running");
        Bundle fitResults = client.search().forResource(Observation.class)
                .where(Observation.CODE.exactly().systemAndCode("http://loinc.org", "57905-2"))
                .count(1).returnBundle(Bundle.class).execute();
        Bundle practitioners = client.search().forResource(Practitioner.class)
                .count(1).returnBundle(Bundle.class).execute();
        assumeTrue(fitResults.hasEntry() && practitioners.hasEntry(), "Seed data is missing");
        Observation source = (Observation) fitResults.getEntryFirstRep().getResource();
        patientId = source.getSubject().getReferenceElement().getIdPart();
        agent = practitioners.getEntryFirstRep().getResource().getIdElement().toUnqualifiedVersionless().getValue();
        service = new PatientSummaryService(client, Clock.systemUTC());
        auditIdsBefore = auditIds();
    }

    @AfterEach
    void cleanUp() {
        if (client == null) {
            return;
        }
        Set<String> added = auditIds();
        added.removeAll(auditIdsBefore);
        added.forEach(id -> client.delete().resourceById("AuditEvent", id).execute());
    }

    @Test
    void summarizesASeededPatientAndAuditsTheRead() {
        PatientSummary summary = service.summarize(patientId, agent);
        assertThat(summary.id()).isEqualTo(patientId);
        assertThat(summary.name()).isNotBlank();
        assertThat(summary.age()).isNotNull();
        assertThat(summary.fitResults()).isNotEmpty();
        Set<String> added = auditIds();
        added.removeAll(auditIdsBefore);
        assertThat(added).hasSize(1);
    }

    private boolean isReachable() {
        try {
            client.capabilities().ofType(CapabilityStatement.class).execute();
            return true;
        } catch (FhirClientConnectionException exception) {
            return false;
        }
    }

    private Set<String> auditIds() {
        Bundle found = client.search().forResource(AuditEvent.class)
                .where(AuditEvent.AGENT.hasId(agent))
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class).execute();
        return found.getEntry().stream()
                .map(entry -> entry.getResource().getIdElement().getIdPart())
                .collect(Collectors.toSet());
    }
}
