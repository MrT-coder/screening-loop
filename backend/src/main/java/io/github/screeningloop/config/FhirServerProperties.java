package io.github.screeningloop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fhir.server")
public record FhirServerProperties(@DefaultValue("http://127.0.0.1:8090/fhir") String url) {
}
