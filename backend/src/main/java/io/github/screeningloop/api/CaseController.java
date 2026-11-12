package io.github.screeningloop.api;

import io.github.screeningloop.cases.CaseService;
import io.github.screeningloop.cases.CaseWithHistory;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private static final String ACTOR_HEADER = "X-Actor";

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseView open(@Valid @RequestBody OpenCaseRequest request) {
        return CaseView.from(service.openCase(
                request.patient(), request.fitResult(), request.owner(), request.dueDate()));
    }

    @PostMapping("/{id}/transitions")
    public CaseView advance(
            @PathVariable String id,
            @RequestHeader(ACTOR_HEADER) String actor,
            @Valid @RequestBody TransitionBody body) {
        return CaseView.from(service.advance(id, body.toRequest(), actor));
    }

    @GetMapping("/{id}")
    public CaseDetail detail(@PathVariable String id, @RequestHeader(ACTOR_HEADER) String actor) {
        CaseWithHistory found = service.findCase(id, actor);
        return CaseDetail.from(found.task(), found.history());
    }

    @GetMapping
    public List<CaseView> listOpen(@RequestHeader(ACTOR_HEADER) String actor) {
        return service.findOpenCases(actor).stream().map(CaseView::from).toList();
    }
}
