package io.github.screeningloop.seed;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
public class SeedRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedRunner.class);

    private final SeedProperties properties;
    private final IGenericClient client;
    private final SyntheaBundleReader reader;
    private final FitResultClassifier classifier;

    public SeedRunner(SeedProperties properties, FhirContext fhirContext, IGenericClient client) {
        this.properties = properties;
        this.client = client;
        this.reader = new SyntheaBundleReader(fhirContext);
        this.classifier = new FitResultClassifier(properties.fitCutoff());
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Path directory = Path.of(properties.syntheaDirectory());
        loadAll(reader.referenceFiles(directory));
        PatientSelection selection = new PatientSelector(reader, classifier)
                .select(reader.patientFiles(directory), properties.backgroundCount());
        loadAll(selection.all());
        LOGGER.info("Seed finished: {} positive FIT patients and {} background patients loaded",
                selection.positives().size(), selection.background().size());
    }

    private void loadAll(List<Path> files) {
        for (Path file : files) {
            Bundle bundle = reader.withoutBillingResources(reader.read(file));
            classifier.addInterpretation(bundle);
            client.transaction().withBundle(bundle).execute();
        }
        LOGGER.info("Loaded {} bundles", files.size());
    }
}
