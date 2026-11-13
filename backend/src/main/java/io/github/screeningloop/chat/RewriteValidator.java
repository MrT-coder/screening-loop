package io.github.screeningloop.chat;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class RewriteValidator {

    private static final int MAX_LENGTH = 700;
    private static final String REASSURANCE = "not a diagnosis";
    private static final Pattern FORBIDDEN = Pattern.compile("[0-9]|https?:|www\\.|@|[<>`]|\\R");

    private final PlaceholderResolver resolver;

    public RewriteValidator(PlaceholderResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isValid(String original, String candidate, Set<String> allowed) {
        return isValid(original, candidate, allowed, REASSURANCE);
    }

    public boolean isValid(String original, String candidate, Set<String> allowed, String reassurance) {
        boolean isWellFormed = candidate != null
                && !candidate.isBlank()
                && candidate.length() <= MAX_LENGTH
                && !FORBIDDEN.matcher(candidate).find();
        return isWellFormed
                && keepsReassurance(original, candidate, reassurance)
                && keepsPlaceholders(original, candidate, allowed);
    }

    private boolean keepsReassurance(String original, String candidate, String reassurance) {
        return !original.toLowerCase(Locale.ROOT).contains(reassurance)
                || candidate.toLowerCase(Locale.ROOT).contains(reassurance);
    }

    private boolean keepsPlaceholders(String original, String candidate, Set<String> allowed) {
        Set<String> found = resolver.placeholdersIn(candidate);
        return found.equals(resolver.placeholdersIn(original)) && allowed.containsAll(found);
    }
}
