package io.github.screeningloop.api;

import io.github.screeningloop.security.AppUser;

public record MeView(String username, String practitioner, String role) {

    public static MeView from(AppUser user) {
        return new MeView(user.getUsername(), user.practitioner(), user.role().name());
    }
}
