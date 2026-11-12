package io.github.screeningloop.seed;

import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

public class FitResultClassifier {

    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String FIT_CODE = "57905-2";
    private static final String INTERPRETATION_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";

    private final double cutoff;

    public FitResultClassifier(double cutoff) {
        this.cutoff = cutoff;
    }

    public boolean hasPositiveResult(Bundle bundle) {
        return fitObservations(bundle).anyMatch(this::isPositive);
    }

    public void addInterpretation(Bundle bundle) {
        fitObservations(bundle).forEach(this::interpret);
    }

    private Stream<Observation> fitObservations(Bundle bundle) {
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .filter(observation -> observation.getCode().hasCoding(LOINC_SYSTEM, FIT_CODE));
    }

    private boolean isPositive(Observation observation) {
        return observation.hasValueQuantity()
                && observation.getValueQuantity().getValue().doubleValue() >= cutoff;
    }

    private void interpret(Observation observation) {
        Coding coding = isPositive(observation)
                ? new Coding(INTERPRETATION_SYSTEM, "POS", "Positive")
                : new Coding(INTERPRETATION_SYSTEM, "NEG", "Negative");
        observation.getInterpretation().clear();
        observation.addInterpretation(new CodeableConcept(coding));
    }
}
