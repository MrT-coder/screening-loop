package io.github.screeningloop.chat;

import java.text.Normalizer;
import java.util.Locale;

final class TextNormalizer {

    private TextNormalizer() {
    }

    static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String lettersOnly = withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        return " " + lettersOnly.trim() + " ";
    }
}
