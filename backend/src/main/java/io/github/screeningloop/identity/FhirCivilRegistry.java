package io.github.screeningloop.identity;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

public class FhirCivilRegistry implements CivilRegistry {

    private static final String DOCUMENT_SYSTEM = "http://hl7.org/fhir/sid/us-ssn";
    private static final int LOOKUP_LIMIT = 2;

    private final IGenericClient client;

    public FhirCivilRegistry(IGenericClient client) {
        this.client = client;
    }

    @Override
    public Optional<RegistryRecord> find(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return Optional.empty();
        }
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(DOCUMENT_SYSTEM, documentNumber))
                .count(LOOKUP_LIMIT)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().size() == 1
                ? toRecord((Patient) bundle.getEntryFirstRep().getResource())
                : Optional.empty();
    }

    private Optional<RegistryRecord> toRecord(Patient patient) {
        boolean hasFullBirthDate = patient.hasBirthDate()
                && patient.getBirthDateElement().getPrecision() == TemporalPrecisionEnum.DAY;
        return hasFullBirthDate
                ? Optional.of(new RegistryRecord(
                        patient.getIdElement().getIdPart(),
                        LocalDate.parse(patient.getBirthDateElement().getValueAsString())))
                : Optional.empty();
    }
}
