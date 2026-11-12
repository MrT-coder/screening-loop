package io.github.screeningloop.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class GroqReplyRewriterTest {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final int MAX_TOKENS = 300;
    private static final String MODEL = "llama-3.1-8b-instant";
    private static final String TEMPLATE = "Your referral was requested. Expected by {{NEXT_STEP_DATE}}.";
    private static final String RESPONSE = """
            {"id":"x","object":"chat.completion","choices":[{"index":0,"finish_reason":"stop",
            "message":{"role":"assistant","content":"  A specialist will see you, by {{NEXT_STEP_DATE}}.  "}}],
            "usage":{"total_tokens":10}}
            """;

    private final RestClient.Builder builder = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Authorization", "Bearer test-key");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GroqReplyRewriter rewriter = new GroqReplyRewriter(builder.build(), MODEL);

    @Test
    void sendsTheTemplateToTheDocumentedEndpointAndReturnsTheTrimmedAnswer() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value(TEMPLATE))
                .andExpect(jsonPath("$.max_completion_tokens").value(MAX_TOKENS))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        assertThat(rewriter.rewrite(TEMPLATE)).contains("A specialist will see you, by {{NEXT_STEP_DATE}}.");
        server.verify();
    }

    private void answerNextCallWith(ResponseCreator response) {
        server.reset();
        server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(response);
    }

    @Test
    void givesNothingBackWhenTheServiceFailsOrIsRateLimited() {
        answerNextCallWith(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(rewriter.rewrite(TEMPLATE)).isEmpty();
        answerNextCallWith(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(rewriter.rewrite(TEMPLATE)).isEmpty();
    }

    @Test
    void givesNothingBackForAnEmptyOrMalformedAnswer() {
        answerNextCallWith(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThat(rewriter.rewrite(TEMPLATE)).isEmpty();
        answerNextCallWith(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThat(rewriter.rewrite(TEMPLATE)).isEmpty();
        answerNextCallWith(withSuccess(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"   \"}}]}",
                MediaType.APPLICATION_JSON));
        assertThat(rewriter.rewrite(TEMPLATE)).isEmpty();
    }
}
