package io.github.screeningloop.llm;

import io.github.screeningloop.chat.ReplyRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmConfiguration.class);
    private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int READ_TIMEOUT_MILLIS = 4_000;

    @Bean
    public ReplyRewriter replyRewriter(LlmProperties properties, RestClient.Builder builder) {
        if (!properties.enabled()) {
            return ReplyRewriter.none();
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            LOGGER.warn("The LLM is enabled but no API key is set, using the fixed templates only");
            return ReplyRewriter.none();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        RestClient client = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .requestFactory(factory)
                .build();
        return new GroqReplyRewriter(client, properties.model());
    }
}
