package io.github.screeningloop.seed;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyntheaBundleReaderTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final SyntheaBundleReader reader = new SyntheaBundleReader(fhirContext);

    @TempDir
    private Path directory;

    @Test
    void removesClaimAndExplanationOfBenefitAndKeepsEverythingElse() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        bundle.addEntry().setResource(new Claim());
        bundle.addEntry().setResource(new ExplanationOfBenefit());

        Bundle result = reader.withoutBillingResources(bundle);

        assertThat(result.getEntry())
                .extracting(entry -> entry.getResource().fhirType())
                .containsExactly("Patient");
    }

    @Test
    void dropsProvenanceTargetsThatPointToRemovedBillingResources() {
        Provenance provenance = new Provenance();
        provenance.addTarget(new Reference("urn:uuid:patient"));
        provenance.addTarget(new Reference("urn:uuid:claim"));
        Bundle bundle = new Bundle();
        bundle.addEntry().setFullUrl("urn:uuid:patient").setResource(new Patient());
        bundle.addEntry().setFullUrl("urn:uuid:claim").setResource(new Claim());
        bundle.addEntry().setFullUrl("urn:uuid:provenance").setResource(provenance);

        reader.withoutBillingResources(bundle);

        assertThat(provenance.getTarget())
                .extracting(Reference::getReference)
                .containsExactly("urn:uuid:patient");
    }

    @Test
    void listsReferenceBundlesApartFromPatientBundles() throws IOException {
        write("hospitalInformation1.json", new Bundle());
        write("practitionerInformation1.json", new Bundle());
        write("Jane_Doe_1.json", new Bundle());

        assertThat(reader.referenceFiles(directory))
                .extracting(file -> file.getFileName().toString())
                .containsExactly("hospitalInformation1.json", "practitionerInformation1.json");
        assertThat(reader.patientFiles(directory))
                .extracting(file -> file.getFileName().toString())
                .containsExactly("Jane_Doe_1.json");
    }

    @Test
    void readsABundleFromDisk() throws IOException {
        Patient patient = new Patient();
        patient.addName().setFamily("Doe");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        write("Jane_Doe_1.json", bundle);

        assertThat(reader.read(directory.resolve("Jane_Doe_1.json")).getEntry()).hasSize(1);
    }

    private void write(String name, Resource resource) throws IOException {
        Files.writeString(directory.resolve(name), fhirContext.newJsonParser().encodeResourceToString(resource));
    }
}
