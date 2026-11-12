package io.github.screeningloop.patient;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import io.github.screeningloop.api.CaseView;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;

public class PatientSummaryBuilder {

    private static final int RECENT_LIMIT = 5;

    public PatientSummary build(PatientRecords records, LocalDate today) {
        Patient patient = records.patient();
        return new PatientSummary(
                patient.getIdElement().getIdPart(),
                patient.getNameFirstRep().getNameAsSingleString(),
                patient.hasGender() ? patient.getGender().toCode() : null,
                ageOf(patient, today),
                fitResults(records),
                conditions(records),
                activeMedications(records),
                recentProcedures(records),
                records.cases().stream().map(CaseView::from).toList());
    }

    private Integer ageOf(Patient patient, LocalDate today) {
        boolean hasFullBirthDate = patient.hasBirthDate()
                && patient.getBirthDateElement().getPrecision() == TemporalPrecisionEnum.DAY;
        if (!hasFullBirthDate) {
            return null;
        }
        return Period.between(LocalDate.parse(patient.getBirthDateElement().getValueAsString()), today).getYears();
    }

    private List<PatientSummary.FitResult> fitResults(PatientRecords records) {
        return records.fitResults().stream()
                .sorted(newestFirst(observation -> observation.getEffectiveDateTimeType().getValue()))
                .map(this::toFitResult)
                .toList();
    }

    private PatientSummary.FitResult toFitResult(Observation observation) {
        boolean hasValue = observation.hasValueQuantity() && observation.getValueQuantity().hasValue();
        return new PatientSummary.FitResult(
                observation.getEffectiveDateTimeType().getValueAsString(),
                hasValue ? observation.getValueQuantity().getValue().doubleValue() : null,
                observation.getValueQuantity().getUnit(),
                observation.getInterpretationFirstRep().getCodingFirstRep().getCode());
    }

    private List<PatientSummary.ConditionEntry> conditions(PatientRecords records) {
        return records.conditions().stream()
                .map(condition -> new PatientSummary.ConditionEntry(
                        nameOf(condition.getCode()),
                        condition.getClinicalStatus().getCodingFirstRep().getCode()))
                .toList();
    }

    private List<String> activeMedications(PatientRecords records) {
        return records.medications().stream()
                .filter(request -> request.getStatus() == MedicationRequest.MedicationRequestStatus.ACTIVE)
                .map(request -> nameOf(request.getMedicationCodeableConcept()))
                .toList();
    }

    private List<PatientSummary.ProcedureEntry> recentProcedures(PatientRecords records) {
        return records.procedures().stream()
                .sorted(newestFirst(procedure -> {
                    DateTimeType performed = performedAt(procedure);
                    return performed == null ? null : performed.getValue();
                }))
                .limit(RECENT_LIMIT)
                .map(this::toProcedureEntry)
                .toList();
    }

    private PatientSummary.ProcedureEntry toProcedureEntry(Procedure procedure) {
        DateTimeType performed = performedAt(procedure);
        return new PatientSummary.ProcedureEntry(
                nameOf(procedure.getCode()), performed == null ? null : performed.getValueAsString());
    }

    private DateTimeType performedAt(Procedure procedure) {
        if (procedure.hasPerformedPeriod()) {
            return procedure.getPerformedPeriod().getStartElement();
        }
        return procedure.hasPerformedDateTimeType() ? procedure.getPerformedDateTimeType() : null;
    }

    private String nameOf(CodeableConcept concept) {
        return concept.hasText() ? concept.getText() : concept.getCodingFirstRep().getDisplay();
    }

    private <T> Comparator<T> newestFirst(Function<T, Date> dateOf) {
        return Comparator.comparing(dateOf, Comparator.nullsLast(Comparator.<Date>reverseOrder()));
    }
}
