package io.github.screeningloop.api;

import io.github.screeningloop.security.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public MeView me(@AuthenticationPrincipal AppUser user) {
        return MeView.from(user);
    }
}
