package io.github.screeningloop.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "screening")
public record DemoUsersProperties(List<UserEntry> users) {

    public record UserEntry(String username, String password, String practitioner, String role) {
    }
}
