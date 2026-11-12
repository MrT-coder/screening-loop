package io.github.screeningloop.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "seed")
public record SeedProperties(
        String syntheaDirectory,
        @DefaultValue("200") int backgroundCount,
        @DefaultValue("100") double fitCutoff) {
}
