package io.github.screeningloop.chat;

import java.util.Arrays;
import java.util.Locale;

public enum Language {
    EN("en", "English"),
    ES("es", "Spanish");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Language fromCode(String value) {
        if (value == null) {
            return EN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(language -> normalized.equals(language.code) || normalized.startsWith(language.code + "-"))
                .findFirst()
                .orElse(EN);
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public Locale locale() {
        return Locale.forLanguageTag(code);
    }
}
