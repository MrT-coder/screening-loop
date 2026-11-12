package io.github.screeningloop.patient;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import io.github.screeningloop.audit.AuditEventFactory;
import io.github.screeningloop.cases.CaseCodes;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Service;

@Service
public class PatientSummaryService {

    private static final String FIT_SYSTEM = "http://loinc.org";
    private static final String FIT_CODE = "57905-2";
    private static final String ACTIVE = "active";
    private static final int PAGE_SIZE = 200;

    private final IGenericClient client;
    private final Clock clock;
    private final PatientSummaryBuilder builder = new PatientSummaryBuilder();
    private final AuditEventFactory auditEvents = new AuditEventFactory();

    public PatientSummaryService(IGenericClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public PatientSummary summarize(String patientId, String agent) {
        Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
        String reference = "Patient/" + patientId;
        PatientRecords records = new PatientRecords(
                patient,
                findByPatient(Observation.class, Observation.PATIENT, reference,
                        Observation.CODE.exactly().systemAndCode(FIT_SYSTEM, FIT_CODE)),
                findByPatient(Condition.class, Condition.PATIENT, reference),
                findByPatient(MedicationRequest.class, MedicationRequest.PATIENT, reference,
                        MedicationRequest.STATUS.exactly().code(ACTIVE)),
                findByPatient(Procedure.class, Procedure.PATIENT, reference),
                findByPatient(Task.class, Task.PATIENT, reference,
                        Task.CODE.exactly().systemAndCode(CaseCodes.TASK_TYPE_SYSTEM, CaseCodes.SCREENING_CASE)));
        client.create().resource(auditEvents.forQuery(agent, reference + "/summary", clock.instant())).execute();
        return builder.build(records, LocalDate.now(clock));
    }

    private <T extends IBaseResource> List<T> findByPatient(
            Class<T> type, ReferenceClientParam param, String reference, ICriterion<?>... extra) {
        IQuery<IBaseBundle> query = client.search().forResource(type).where(param.hasId(reference));
        for (ICriterion<?> criterion : extra) {
            query = query.and(criterion);
        }
        Bundle bundle = query.count(PAGE_SIZE)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream().map(entry -> type.cast(entry.getResource())).toList();
    }
}
