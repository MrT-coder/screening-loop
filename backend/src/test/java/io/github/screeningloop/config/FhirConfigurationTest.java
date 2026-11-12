package io.github.screeningloop.config;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FhirConfigurationTest {

    @Autowired
    private FhirContext fhirContext;

    @Test
    void exposesAnR4ContextWithTheExpectedSpecificationVersion() {
        assertThat(fhirContext.getVersion().getVersion()).isEqualTo(FhirVersionEnum.R4);
        assertThat(fhirContext.getVersion().getVersion().getFhirVersionString()).isEqualTo("4.0.1");
    }
}
