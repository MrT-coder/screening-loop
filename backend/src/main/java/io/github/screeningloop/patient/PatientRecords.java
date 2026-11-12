package io.github.screeningloop.patient;

import java.util.List;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Task;

public record PatientRecords(
        Patient patient,
        List<Observation> fitResults,
        List<Condition> conditions,
        List<MedicationRequest> medications,
        List<Procedure> procedures,
        List<Task> cases) {
}
