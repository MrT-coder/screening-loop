package io.github.screeningloop.chat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.cases.CaseStage;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MessageCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{[A-Z_]+}}");
    private static final Pattern FORMAT_SPECIFIER = Pattern.compile("%[sd]");

    private static Stream<String> catalogs() {
        return Stream.of("patient", "clinician");
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    @Test
    void definesTheSameKeysInEveryLanguage() {
        catalogs().forEach(name -> assertThat(MessageCatalog.load(name, Language.ES).keys())
                .as(name).isEqualTo(MessageCatalog.load(name, Language.EN).keys()));
    }

    @Test
    void keepsTheSamePlaceholdersAndFormatSpecifiersInEveryLanguage() {
        catalogs().forEach(name -> {
            MessageCatalog english = MessageCatalog.load(name, Language.EN);
            MessageCatalog spanish = MessageCatalog.load(name, Language.ES);
            for (String key : english.keys()) {
                assertThat(matches(PLACEHOLDER, spanish.get(key))).as(name + "." + key)
                        .isEqualTo(matches(PLACEHOLDER, english.get(key)));
                assertThat(matches(FORMAT_SPECIFIER, spanish.get(key))).as(name + "." + key)
                        .isEqualTo(matches(FORMAT_SPECIFIER, english.get(key)));
            }
        });
    }

    @Test
    void hasATemplateAndALabelForEveryCaseStageInEveryLanguage() {
        for (Language language : Language.values()) {
            ReplyTemplates templates = ReplyTemplates.of(language);
            for (CaseStage stage : CaseStage.values()) {
                assertThat(templates.forStage(stage)).as(language + " " + stage.code()).isNotBlank();
                assertThat(templates.stageLabel(stage)).as(language + " " + stage.code()).isNotBlank();
            }
        }
    }

    @Test
    void readsSpanishAccentsAsUtf8() {
        String phrase = ReplyTemplates.of(Language.ES).reassurancePhrase();
        assertThat(phrase).startsWith("no es un diagn");
        assertThat(phrase.chars().anyMatch(character -> character > Byte.MAX_VALUE)).isTrue();
    }

    @Test
    void fallsBackToEnglishForMissingOrUnknownLanguageCodes() {
        assertThat(Language.fromCode(null)).isEqualTo(Language.EN);
        assertThat(Language.fromCode("xx")).isEqualTo(Language.EN);
        assertThat(Language.fromCode(" ES ")).isEqualTo(Language.ES);
        assertThat(Language.fromCode("es-EC")).isEqualTo(Language.ES);
    }
}
