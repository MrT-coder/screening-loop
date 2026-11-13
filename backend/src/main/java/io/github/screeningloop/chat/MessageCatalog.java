package io.github.screeningloop.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

final class MessageCatalog {

    private final Properties entries;

    private MessageCatalog(Properties entries) {
        this.entries = entries;
    }

    static MessageCatalog load(String baseName, Language language) {
        String path = "/chat/" + baseName + "_" + language.code() + ".properties";
        try (InputStream stream = MessageCatalog.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing message catalog " + path);
            }
            Properties entries = new Properties();
            entries.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return new MessageCatalog(entries);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    String get(String key) {
        String value = entries.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing message " + key);
        }
        return value;
    }

    Set<String> keys() {
        return entries.stringPropertyNames();
    }
}
