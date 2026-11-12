package io.github.screeningloop.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

class FitResultClassifierTest {

    private static final double CUTOFF = 100.0;
    private static final double POSITIVE_VALUE = 173.21;
    private static final double NEGATIVE_VALUE = 24.5;
    private static final String INTERPRETATION_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";

    private final FitResultClassifier classifier = new FitResultClassifier(CUTOFF);

    @Test
    void flagsABundleWithAFitAtOrAboveTheCutoffAsPositive() {
        assertThat(classifier.hasPositiveResult(bundleWith(fit(POSITIVE_VALUE)))).isTrue();
        assertThat(classifier.hasPositiveResult(bundleWith(fit(CUTOFF)))).isTrue();
    }

    @Test
    void doesNotFlagAFitBelowTheCutoff() {
        assertThat(classifier.hasPositiveResult(bundleWith(fit(NEGATIVE_VALUE)))).isFalse();
    }

    @Test
    void ignoresObservationsThatAreNotAFit() {
        Observation other = fit(POSITIVE_VALUE);
        other.getCode().getCodingFirstRep().setCode("1234-5");
        assertThat(classifier.hasPositiveResult(bundleWith(other))).isFalse();
    }

    @Test
    void writesPositiveAndNegativeInterpretationCodes() {
        Observation positive = fit(POSITIVE_VALUE);
        Observation negative = fit(NEGATIVE_VALUE);
        classifier.addInterpretation(bundleWith(positive, negative));
        assertThat(interpretationCode(positive)).isEqualTo("POS");
        assertThat(interpretationCode(negative)).isEqualTo("NEG");
        assertThat(positive.getInterpretationFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo(INTERPRETATION_SYSTEM);
    }

    private String interpretationCode(Observation observation) {
        return observation.getInterpretationFirstRep().getCodingFirstRep().getCode();
    }

    private Observation fit(double value) {
        Observation observation = new Observation();
        observation.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "57905-2", "FIT")));
        observation.setValue(new Quantity().setValue(value).setUnit("ng/mL"));
        return observation;
    }

    private Bundle bundleWith(Observation... observations) {
        Bundle bundle = new Bundle();
        for (Observation observation : observations) {
            bundle.addEntry().setResource(observation);
        }
        return bundle;
    }
}
