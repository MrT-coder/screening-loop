package io.github.screeningloop.chat;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z_]+)}}");

    public String resolve(String template, Map<String, String> values, Set<String> allowed) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!allowed.contains(name)) {
                throw new IllegalArgumentException("Placeholder is not allowed here: " + name);
            }
            if (!values.containsKey(name)) {
                throw new IllegalArgumentException("Missing value for placeholder: " + name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(values.get(name)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Set<String> placeholdersIn(String template) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
