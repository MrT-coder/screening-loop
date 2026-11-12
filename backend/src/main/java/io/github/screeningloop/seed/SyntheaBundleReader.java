package io.github.screeningloop.seed;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Resource;

public class SyntheaBundleReader {

    private static final String HOSPITAL_FILE_PREFIX = "hospitalInformation";
    private static final String PRACTITIONER_FILE_PREFIX = "practitionerInformation";
    private static final String JSON_EXTENSION = ".json";

    private final FhirContext fhirContext;

    public SyntheaBundleReader(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public List<Path> referenceFiles(Path directory) {
        return listJsonFiles(directory).stream()
                .filter(this::isReferenceFile)
                .sorted()
                .toList();
    }

    public List<Path> patientFiles(Path directory) {
        return listJsonFiles(directory).stream()
                .filter(file -> !isReferenceFile(file))
                .sorted()
                .toList();
    }

    public Bundle read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return fhirContext.newJsonParser().parseResource(Bundle.class, reader);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read bundle " + file.getFileName(), exception);
        }
    }

    public Bundle withoutBillingResources(Bundle bundle) {
        Set<String> removedUrls = bundle.getEntry().stream()
                .filter(entry -> isBillingResource(entry.getResource()))
                .map(Bundle.BundleEntryComponent::getFullUrl)
                .collect(Collectors.toSet());
        bundle.getEntry().removeIf(entry -> isBillingResource(entry.getResource()));
        bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Provenance.class::isInstance)
                .map(Provenance.class::cast)
                .forEach(provenance -> provenance.getTarget()
                        .removeIf(target -> removedUrls.contains(target.getReference())));
        return bundle;
    }

    private List<Path> listJsonFiles(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(JSON_EXTENSION)).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list directory " + directory, exception);
        }
    }

    private boolean isReferenceFile(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith(HOSPITAL_FILE_PREFIX) || name.startsWith(PRACTITIONER_FILE_PREFIX);
    }

    private boolean isBillingResource(Resource resource) {
        return resource instanceof Claim || resource instanceof ExplanationOfBenefit;
    }
}
