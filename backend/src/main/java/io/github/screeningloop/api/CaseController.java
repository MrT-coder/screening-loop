package io.github.screeningloop.api;

import io.github.screeningloop.cases.CaseService;
import io.github.screeningloop.cases.CaseWithHistory;
import io.github.screeningloop.security.AppUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseView open(@Valid @RequestBody OpenCaseRequest request, @AuthenticationPrincipal AppUser user) {
        return CaseView.from(service.openCase(
                request.patient(), request.fitResult(), request.owner(), request.dueDate(), user.actor()));
    }

    @PostMapping("/{id}/transitions")
    public CaseView advance(
            @PathVariable String id,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody TransitionBody body) {
        return CaseView.from(service.advance(id, body.toRequest(), user.actor()));
    }

    @GetMapping("/{id}")
    public CaseDetail detail(@PathVariable String id, @AuthenticationPrincipal AppUser user) {
        CaseWithHistory found = service.findCase(id, user.practitioner());
        return CaseDetail.from(found.task(), found.history());
    }

    @GetMapping
    public List<CaseView> listOpen(@AuthenticationPrincipal AppUser user) {
        return service.findOpenCases(user.practitioner()).stream().map(CaseView::from).toList();
    }
}
