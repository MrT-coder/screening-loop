package io.github.screeningloop.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseServiceIntegrationTest {

    private static final String SERVER_URL = "http://127.0.0.1:8090/fhir";
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 1);

    private final List<IdType> created = new ArrayList<>();
    private IGenericClient client;
    private CaseService service;
    private String patient;
    private String report;
    private String agent;
    private Set<String> auditIdsBefore;

    @BeforeEach
    void connect() {
        client = FhirContext.forR4Cached().newRestfulGenericClient(SERVER_URL);
        boolean isServerUp = isReachable();
        assumeTrue(isServerUp, "HAPI server is not running");
        service = new CaseService(client, Clock.systemUTC());
        Bundle reports = firstOf(DiagnosticReport.class);
        Bundle practitioners = firstOf(Practitioner.class);
        assumeTrue(reports.hasEntry() && practitioners.hasEntry(), "Seed data is missing");
        DiagnosticReport source = (DiagnosticReport) reports.getEntryFirstRep().getResource();
        report = source.getIdElement().toUnqualifiedVersionless().getValue();
        patient = source.getSubject().getReference();
        agent = practitioners.getEntryFirstRep().getResource().getIdElement().toUnqualifiedVersionless().getValue();
        auditIdsBefore = auditIds();
    }

    private Bundle firstOf(Class<? extends IBaseResource> type) {
        return client.search().forResource(type).count(1).returnBundle(Bundle.class).execute();
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

    @AfterEach
    void cleanUp() {
        if (client == null) {
            return;
        }
        newAuditIds().forEach(id -> client.delete().resourceById("AuditEvent", id).execute());
        for (int index = created.size() - 1; index >= 0; index--) {
            client.delete().resourceById(created.get(index)).execute();
        }
    }

    @Test
    void opensAdvancesAndLeavesProvenanceAndAudit() {
        Task opened = service.openCase(patient, report, agent, DUE_DATE);
        created.add(opened.getIdElement().toUnqualifiedVersionless());
        TransitionRequest request = TransitionRequest.advance(CaseStage.PATIENT_NOTIFIED, agent, DUE_DATE);
        Task advanced = service.advance(opened.getIdElement().getIdPart(), request, agent);
        assertThat(advanced.getBusinessStatus().getCodingFirstRep().getCode()).isEqualTo("patient-notified");
        assertThat(advanced.getMeta().getVersionId()).isEqualTo("2");
        trackProvenance(advanced);
        assertThat(advanced.getRelevantHistoryFirstRep().getReference()).startsWith("Provenance/");
        assertThat(service.findOpenCases(agent)).extracting(task -> task.getIdElement().getIdPart())
                .contains(opened.getIdElement().getIdPart());
        assertThat(newAuditIds()).hasSize(1);
    }

    @Test
    void rejectsAStaleTransitionWithoutWritingAnything() {
        Task opened = service.openCase(patient, report, agent, DUE_DATE);
        created.add(opened.getIdElement().toUnqualifiedVersionless());
        TransitionRequest skip = TransitionRequest.advance(CaseStage.APPOINTMENT_BOOKED, agent, DUE_DATE);
        assertThatThrownBy(() -> service.advance(opened.getIdElement().getIdPart(), skip, agent))
                .isInstanceOf(InvalidTransitionException.class);
        Task unchanged = client.read().resource(Task.class).withId(opened.getIdElement().getIdPart()).execute();
        assertThat(unchanged.getMeta().getVersionId()).isEqualTo("1");
    }

    private boolean isReachable() {
        try {
            client.capabilities().ofType(CapabilityStatement.class).execute();
            return true;
        } catch (FhirClientConnectionException exception) {
            return false;
        }
    }

    private void trackProvenance(Task task) {
        String reference = task.getRelevantHistoryFirstRep().getReference();
        created.add(new IdType(reference).toUnqualifiedVersionless());
        Bundle found = client.search().forResource(Provenance.class)
                .where(Provenance.TARGET.hasId(task.getIdElement().toUnqualifiedVersionless().getValue()))
                .returnBundle(Bundle.class).execute();
        assertThat(found.getEntry()).hasSize(1);
    }

    private Set<String> newAuditIds() {
        Set<String> added = auditIds();
        added.removeAll(auditIdsBefore);
        return added;
    }
}
