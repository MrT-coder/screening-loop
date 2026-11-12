package io.github.screeningloop.chat;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class IntentClassifier {

    private static final List<Rule> RULES = List.of(
            new Rule(Intent.UNSUPPORTED, List.of(
                    "should i", "do i have", "is it serious", "will i die", "am i going to", "how long do i have",
                    "what should", "can i take", "stop taking", "diagnos", "prognos", "symptom", "sintoma",
                    "treatment", "tratamiento", "surger", "pain", "dolor", "debo", "deberia", "morir", "die ")),
            new Rule(Intent.MEDICATIONS, List.of(
                    "medic", "prescription", "pill", "drug", "receta")),
            new Rule(Intent.LATEST_EXAMS, List.of(
                    "exam", "test", "lab ", "labs ", "result", "procedure", "colonoscop", "fit ", "analisis",
                    "prueba")),
            new Rule(Intent.CASE_STATUS, List.of(
                    "status", "case", "follow", "referral", "appointment", "next step", "screening", "progress",
                    "stage", "estado", "caso", "cita", "derivacion")),
            new Rule(Intent.HELP, List.of("help", "what can you", "what can i ask", "options", "ayuda")),
            new Rule(Intent.GREETING, List.of("hello", "hey ", "hi ", "hola", "good morning", "buenas")));

    public Intent classify(String text) {
        if (text == null || text.isBlank()) {
            return Intent.UNSUPPORTED;
        }
        String normalized = normalize(text);
        return RULES.stream()
                .filter(rule -> rule.matches(normalized))
                .map(Rule::intent)
                .findFirst()
                .orElse(Intent.UNSUPPORTED);
    }

    private String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String lettersOnly = withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        return " " + lettersOnly.trim() + " ";
    }

    private record Rule(Intent intent, List<String> keywords) {

        boolean matches(String normalized) {
            return keywords.stream().anyMatch(keyword -> normalized.contains(" " + keyword));
        }
    }
}
