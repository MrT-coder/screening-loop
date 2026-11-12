package io.github.screeningloop.security;

import io.github.screeningloop.access.Role;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DemoUserDetailsService implements UserDetailsService {

    private final Map<String, StoredUser> users;

    public DemoUserDetailsService(DemoUsersProperties properties, PasswordEncoder encoder) {
        this.users = properties.users().stream()
                .map(entry -> new StoredUser(
                        entry.username(),
                        encoder.encode(entry.password()),
                        entry.practitioner(),
                        Role.valueOf(entry.role())))
                .collect(Collectors.toMap(StoredUser::username, Function.identity()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        StoredUser stored = users.get(username);
        if (stored == null) {
            throw new UsernameNotFoundException("Unknown user");
        }
        return new AppUser(stored.username(), stored.encodedPassword(), stored.practitioner(), stored.role());
    }

    private record StoredUser(String username, String encodedPassword, String practitioner, Role role) {
    }
}
