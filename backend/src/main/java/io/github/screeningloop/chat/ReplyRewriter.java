package io.github.screeningloop.chat;

import java.util.Optional;

@FunctionalInterface
public interface ReplyRewriter {

    Optional<String> rewrite(String template);

    static ReplyRewriter none() {
        return template -> Optional.empty();
    }
}
