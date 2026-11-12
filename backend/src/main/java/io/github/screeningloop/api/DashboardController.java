package io.github.screeningloop.api;

import io.github.screeningloop.dashboard.DashboardService;
import io.github.screeningloop.dashboard.DashboardSummary;
import io.github.screeningloop.security.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardSummary summarize(@AuthenticationPrincipal AppUser user) {
        return service.summarize(user.practitioner());
    }
}
