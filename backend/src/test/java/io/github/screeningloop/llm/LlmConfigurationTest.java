package io.github.screeningloop.llm;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.chat.ReplyRewriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class LlmConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LlmConfiguration.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void isOffByDefault() {
        runner.run(context -> assertThat(context.getBean(ReplyRewriter.class))
                .isNotInstanceOf(GroqReplyRewriter.class));
    }

    @Test
    void staysOffWhenEnabledWithoutAnApiKey() {
        runner.withPropertyValues("screening.llm.enabled=true")
                .run(context -> assertThat(context.getBean(ReplyRewriter.class))
                        .isNotInstanceOf(GroqReplyRewriter.class));
        runner.withPropertyValues("screening.llm.enabled=true", "screening.llm.api-key=   ")
                .run(context -> assertThat(context.getBean(ReplyRewriter.class))
                        .isNotInstanceOf(GroqReplyRewriter.class));
    }

    @Test
    void staysOffWhenAKeyIsSetButTheSwitchIsNot() {
        runner.withPropertyValues("screening.llm.api-key=secret")
                .run(context -> assertThat(context.getBean(ReplyRewriter.class))
                        .isNotInstanceOf(GroqReplyRewriter.class));
    }

    @Test
    void turnsOnOnlyWhenEnabledAndAKeyIsPresent() {
        runner.withPropertyValues("screening.llm.enabled=true", "screening.llm.api-key=secret")
                .run(context -> assertThat(context.getBean(ReplyRewriter.class))
                        .isInstanceOf(GroqReplyRewriter.class));
    }
}
