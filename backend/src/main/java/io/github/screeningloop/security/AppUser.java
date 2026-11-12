package io.github.screeningloop.security;

import io.github.screeningloop.access.Actor;
import io.github.screeningloop.access.Role;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AppUser extends User {

    private static final long serialVersionUID = 1L;

    private final String practitioner;
    private final Role role;

    public AppUser(String username, String password, String practitioner, Role role) {
        super(username, password, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        this.practitioner = practitioner;
        this.role = role;
    }

    public Actor actor() {
        return new Actor(practitioner, role);
    }

    public String practitioner() {
        return practitioner;
    }

    public Role role() {
        return role;
    }
}
