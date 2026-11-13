package io.github.screeningloop.chat;

import io.github.screeningloop.cases.CaseStage;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

public final class ReplyTemplates {

    private static final String CATALOG = "patient";
    private static final Map<Language, ReplyTemplates> BY_LANGUAGE = loadAll();

    private final Language language;
    private final MessageCatalog catalog;

    private ReplyTemplates(Language language) {
        this.language = language;
        this.catalog = MessageCatalog.load(CATALOG, language);
    }

    public static ReplyTemplates of(Language language) {
        return BY_LANGUAGE.get(language);
    }

    private static Map<Language, ReplyTemplates> loadAll() {
        Map<Language, ReplyTemplates> loaded = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            loaded.put(language, new ReplyTemplates(language));
        }
        return loaded;
    }

    public Language language() {
        return language;
    }

    public String greeting() {
        return catalog.get("greeting");
    }

    public String help() {
        return catalog.get("help");
    }

    public String refusal() {
        return catalog.get("refusal");
    }

    public String noCase() {
        return catalog.get("noCase");
    }

    public String noMedications() {
        return catalog.get("noMedications");
    }

    public String noExams() {
        return catalog.get("noExams");
    }

    public String medications() {
        return catalog.get("medications");
    }

    public String fitPositive() {
        return catalog.get("fitPositive");
    }

    public String fitNegative() {
        return catalog.get("fitNegative");
    }

    public String fitUnknown() {
        return catalog.get("fitUnknown");
    }

    public String overdue() {
        return catalog.get("overdue");
    }

    public String procedures() {
        return catalog.get("procedures");
    }

    public String pendingDate() {
        return catalog.get("pendingDate");
    }

    public String reassurancePhrase() {
        return catalog.get("reassurance");
    }

    public DateTimeFormatter dateFormat() {
        return DateTimeFormatter.ofPattern(catalog.get("datePattern"), language.locale());
    }

    public String forStage(CaseStage stage) {
        return catalog.get("stage." + stage.code());
    }

    public String stageLabel(CaseStage stage) {
        return catalog.get("label." + stage.code());
    }
}
