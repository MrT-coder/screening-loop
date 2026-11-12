package io.github.screeningloop.patient;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.cases.CaseTaskMapper;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;

class PatientSummaryBuilderTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final double FIRST_VALUE = 120.5;
    private static final double SECOND_VALUE = 267.2;
    private static final int RECENT_LIMIT = 5;
    private static final int PROCEDURE_COUNT = 7;
    private static final int EXPECTED_AGE = 64;

    private final PatientSummaryBuilder builder = new PatientSummaryBuilder();

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId("Patient/p1");
        patient.addName().setFamily("Greenholt").addGiven("Ana");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setBirthDateElement(new DateType("1962-06-28"));
        return patient;
    }

    private PatientRecords records(
            List<Observation> fit, List<Condition> conditions, List<MedicationRequest> medications,
            List<Procedure> procedures, List<Task> cases) {
        return new PatientRecords(patient(), fit, conditions, medications, procedures, cases);
    }

    private PatientRecords empty() {
        return records(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private Observation fit(String date, double value, String interpretation) {
        Observation observation = new Observation();
        observation.setEffective(new DateTimeType(date));
        observation.setValue(new Quantity().setValue(value).setUnit("ng/mL"));
        observation.addInterpretation(new CodeableConcept(new Coding("sys", interpretation, null)));
        return observation;
    }

    private Procedure procedure(String name, String start) {
        Procedure procedure = new Procedure();
        procedure.setCode(new CodeableConcept().setText(name));
        procedure.setPerformed(new Period().setStartElement(new DateTimeType(start)));
        return procedure;
    }

    @Test
    void describesThePatientWithAgeComputedFromTheGivenDate() {
        PatientSummary summary = builder.build(empty(), TODAY);
        assertThat(summary.id()).isEqualTo("p1");
        assertThat(summary.name()).isEqualTo("Ana Greenholt");
        assertThat(summary.gender()).isEqualTo("female");
        assertThat(summary.age()).isEqualTo(EXPECTED_AGE);
    }

    @Test
    void listsFitResultsNewestFirst() {
        PatientRecords records = records(
                List.of(fit("2021-03-01", FIRST_VALUE, "NEG"), fit("2022-07-05", SECOND_VALUE, "POS")),
                List.of(), List.of(), List.of(), List.of());
        PatientSummary summary = builder.build(records, TODAY);
        assertThat(summary.fitResults()).extracting(PatientSummary.FitResult::value)
                .containsExactly(SECOND_VALUE, FIRST_VALUE);
        assertThat(summary.fitResults().get(0).interpretation()).isEqualTo("POS");
        assertThat(summary.fitResults().get(0).unit()).isEqualTo("ng/mL");
    }

    @Test
    void listsConditionsWithTheirClinicalStatus() {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept().setText("Polyp of colon (disorder)"));
        condition.setClinicalStatus(new CodeableConcept(new Coding(
                "http://terminology.hl7.org/CodeSystem/condition-clinical", "resolved", null)));
        PatientSummary summary = builder.build(
                records(List.of(), List.of(condition), List.of(), List.of(), List.of()), TODAY);
        assertThat(summary.conditions()).singleElement().satisfies(entry -> {
            assertThat(entry.name()).isEqualTo("Polyp of colon (disorder)");
            assertThat(entry.status()).isEqualTo("resolved");
        });
    }

    @Test
    void includesOnlyActiveMedications() {
        MedicationRequest active = medication("Aspirin 81 MG", MedicationRequest.MedicationRequestStatus.ACTIVE);
        MedicationRequest stopped = medication("Warfarin", MedicationRequest.MedicationRequestStatus.STOPPED);
        PatientSummary summary = builder.build(
                records(List.of(), List.of(), List.of(active, stopped), List.of(), List.of()), TODAY);
        assertThat(summary.activeMedications()).containsExactly("Aspirin 81 MG");
    }

    @Test
    void keepsOnlyTheMostRecentProceduresNewestFirst() {
        List<Procedure> procedures = IntStream.rangeClosed(1, PROCEDURE_COUNT)
                .mapToObj(index -> procedure("Procedure " + index, "2020-0" + index + "-01"))
                .toList();
        PatientSummary summary = builder.build(
                records(List.of(), List.of(), List.of(), procedures, List.of()), TODAY);
        assertThat(summary.recentProcedures()).hasSize(RECENT_LIMIT);
        assertThat(summary.recentProcedures().get(0).name()).isEqualTo("Procedure 7");
    }

    @Test
    void includesTheCasesOfThePatient() {
        Task task = new CaseTaskMapper(new CaseTransitionPolicy()).openCase(
                "Patient/p1", "Observation/fit-1", "Practitioner/gp-1", TODAY, Instant.EPOCH);
        task.setId("t1");
        task.getMeta().setVersionId("1");
        PatientSummary summary = builder.build(
                records(List.of(), List.of(), List.of(), List.of(), List.of(task)), TODAY);
        assertThat(summary.cases()).singleElement().satisfies(view -> {
            assertThat(view.id()).isEqualTo("t1");
            assertThat(view.stage()).isEqualTo("result-issued");
        });
    }

    private MedicationRequest medication(String name, MedicationRequest.MedicationRequestStatus status) {
        MedicationRequest request = new MedicationRequest();
        request.setStatus(status);
        request.setMedication(new CodeableConcept().setText(name));
        return request;
    }
}
