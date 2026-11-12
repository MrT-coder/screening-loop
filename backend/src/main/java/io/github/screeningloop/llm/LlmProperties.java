package io.github.screeningloop.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "screening.llm")
public record LlmProperties(
        @DefaultValue("false") boolean enabled,
        String apiKey,
        @DefaultValue("llama-3.1-8b-instant") String model,
        @DefaultValue("https://api.groq.com/openai/v1") String baseUrl) {
}
