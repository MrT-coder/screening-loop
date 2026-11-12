package io.github.screeningloop.api;

import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import io.github.screeningloop.security.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientSummaryService service;

    public PatientController(PatientSummaryService service) {
        this.service = service;
    }

    @GetMapping("/{id:[A-Za-z0-9.-]{1,64}}/summary")
    public PatientSummary summary(@PathVariable String id, @AuthenticationPrincipal AppUser user) {
        return service.summarize(id, user.practitioner());
    }
}
