package io.github.screeningloop.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.screeningloop.chat.Language;
import io.github.screeningloop.chat.ReplyRewriter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class GroqReplyRewriter implements ReplyRewriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroqReplyRewriter.class);
    private static final double TEMPERATURE = 0.3;
    private static final int MAX_COMPLETION_TOKENS = 300;
    private static final String SYSTEM_PROMPT =
            "You rewrite short messages that a care team sends to a patient in a colorectal cancer screening "
                    + "follow-up. Rewrite the message the user sends in warm, plain, reassuring %s, in at most "
                    + "three short sentences. Rules: keep every placeholder written like {{NAME}} exactly as it "
                    + "appears; never add placeholders, facts, numbers, dates, advice or promises; keep the meaning "
                    + "identical; if the message says that a positive result is not a diagnosis, keep that "
                    + "sentence; reply with the rewritten message only.";

    private final RestClient client;
    private final String model;

    public GroqReplyRewriter(RestClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public Optional<String> rewrite(String template) {
        return rewrite(template, Language.EN);
    }

    @Override
    public Optional<String> rewrite(String template, Language language) {
        CompletionRequest request = new CompletionRequest(
                model,
                List.of(new Message("system", SYSTEM_PROMPT.formatted(language.displayName())),
                        new Message("user", template)),
                TEMPERATURE,
                MAX_COMPLETION_TOKENS);
        try {
            CompletionResponse response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CompletionResponse.class);
            return firstContent(response);
        } catch (RestClientException exception) {
            LOGGER.warn("LLM rewrite failed, using the fixed template: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Optional<String> firstContent(CompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return Optional.empty();
        }
        Message message = response.choices().get(0).message();
        return Optional.ofNullable(message == null ? null : message.content())
                .map(String::trim)
                .filter(content -> !content.isEmpty());
    }

    record CompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("max_completion_tokens") int maxCompletionTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }
}
