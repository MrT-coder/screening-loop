package io.github.screeningloop.chat;

import java.util.Optional;

@FunctionalInterface
public interface ReplyRewriter {

    Optional<String> rewrite(String template);

    default Optional<String> rewrite(String template, Language language) {
        return rewrite(template);
    }

    static ReplyRewriter none() {
        return template -> Optional.empty();
    }
}
