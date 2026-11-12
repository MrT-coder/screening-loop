package io.github.screeningloop.security;

import io.github.screeningloop.access.Actor;
import io.github.screeningloop.access.Role;

public final class TestUsers {

    public static final String PRACTITIONER = "Practitioner/gp-1";

    private TestUsers() {
    }

    public static AppUser coordinator() {
        return new AppUser("coordinator", "secret", PRACTITIONER, Role.COORDINATOR);
    }

    public static AppUser clinician() {
        return new AppUser("clinician", "secret", PRACTITIONER, Role.CLINICIAN);
    }

    public static AppUser guest() {
        return new AppUser("guest", "secret", PRACTITIONER, Role.GUEST);
    }

    public static Actor coordinatorActor() {
        return new Actor(PRACTITIONER, Role.COORDINATOR);
    }
}
