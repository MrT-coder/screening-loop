package io.github.screeningloop.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FhirConfiguration {

    private static final int SOCKET_TIMEOUT_MILLIS = 300_000;

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4Cached();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext, FhirServerProperties properties) {
        fhirContext.getRestfulClientFactory().setSocketTimeout(SOCKET_TIMEOUT_MILLIS);
        return fhirContext.newRestfulGenericClient(properties.url());
    }
}
