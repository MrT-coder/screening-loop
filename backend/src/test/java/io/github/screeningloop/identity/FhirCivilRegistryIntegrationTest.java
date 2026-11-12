package io.github.screeningloop.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FhirCivilRegistryIntegrationTest {

    private static final String SERVER_URL = "http://127.0.0.1:8090/fhir";
    private static final String DOCUMENT_SYSTEM = "http://hl7.org/fhir/sid/us-ssn";

    private IGenericClient client;
    private FhirCivilRegistry registry;
    private Patient patient;
    private String document;

    @BeforeEach
    void connect() {
        client = FhirContext.forR4Cached().newRestfulGenericClient(SERVER_URL);
        assumeTrue(isReachable(), "HAPI server is not running");
        Bundle patients = client.search().forResource(Patient.class)
                .where(Patient.IDENTIFIER.hasSystemWithAnyCode(DOCUMENT_SYSTEM))
                .count(1).returnBundle(Bundle.class).execute();
        assumeTrue(patients.hasEntry(), "Seed data is missing");
        patient = (Patient) patients.getEntryFirstRep().getResource();
        document = patient.getIdentifier().stream()
                .filter(identifier -> DOCUMENT_SYSTEM.equals(identifier.getSystem()))
                .map(Identifier::getValue)
                .findFirst().orElseThrow();
        registry = new FhirCivilRegistry(client);
    }

    @Test
    void findsASeededPatientByDocumentNumber() {
        CivilRegistry.RegistryRecord found = registry.find(document).orElseThrow();
        assertThat(found.patientId()).isEqualTo(patient.getIdElement().getIdPart());
        assertThat(found.birthDate())
                .isEqualTo(LocalDate.parse(patient.getBirthDateElement().getValueAsString()));
    }

    @Test
    void findsNobodyForAnUnknownOrBlankDocument() {
        assertThat(registry.find("000-00-0000")).isEmpty();
        assertThat(registry.find(" ")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }

    private boolean isReachable() {
        try {
            client.capabilities().ofType(CapabilityStatement.class).execute();
            return true;
        } catch (FhirClientConnectionException exception) {
            return false;
        }
    }
}
