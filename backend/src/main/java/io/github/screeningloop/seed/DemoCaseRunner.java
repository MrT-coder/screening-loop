package io.github.screeningloop.seed;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.github.screeningloop.cases.CaseCodes;
import io.github.screeningloop.cases.CaseService;
import io.github.screeningloop.cases.TransitionRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo-cases")
public class DemoCaseRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoCaseRunner.class);
    private static final String FIT_CODE = "57905-2";
    private static final String POSITIVE = "POS";
    private static final int PAGE_SIZE = 200;
    private static final int OWNER_COUNT = 4;

    private final IGenericClient client;
    private final CaseService caseService;
    private final Clock clock;

    public DemoCaseRunner(IGenericClient client, CaseService caseService, Clock clock) {
        this.client = client;
        this.caseService = caseService;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (hasCases()) {
            LOGGER.info("Demo cases skipped: screening cases already exist");
            return;
        }
        List<DemoCasePlan> plans = new DemoCasePlanner(findOwners()).plan(findInputs(), LocalDate.now(clock));
        plans.forEach(this::execute);
        LOGGER.info("Demo cases created: {}", plans.size());
    }

    private void execute(DemoCasePlan plan) {
        Task task = caseService.openCase(plan.patient(), plan.fitResult(), plan.owner(), plan.dueDate());
        String id = task.getIdElement().getIdPart();
        for (TransitionRequest step : plan.steps()) {
            caseService.advance(id, step, step.owner());
        }
    }

    private boolean hasCases() {
        Bundle bundle = client.search()
                .forResource(Task.class)
                .where(Task.CODE.exactly().systemAndCode(CaseCodes.TASK_TYPE_SYSTEM, CaseCodes.SCREENING_CASE))
                .count(1)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return !bundle.getEntry().isEmpty();
    }

    private List<String> findOwners() {
        Bundle bundle = client.search()
                .forResource(Practitioner.class)
                .count(OWNER_COUNT)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(entry -> "Practitioner/" + entry.getResource().getIdElement().getIdPart())
                .toList();
    }

    private List<DemoCaseInput> findInputs() {
        Bundle bundle = client.search()
                .forResource(Observation.class)
                .where(Observation.CODE.exactly().code(FIT_CODE))
                .count(PAGE_SIZE)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(entry -> (Observation) entry.getResource())
                .filter(this::isPositive)
                .map(this::toInput)
                .toList();
    }

    private boolean isPositive(Observation observation) {
        return observation.getInterpretationFirstRep().getCodingFirstRep().getCode().equals(POSITIVE);
    }

    private DemoCaseInput toInput(Observation observation) {
        String patient = observation.getSubject().getReference();
        return new DemoCaseInput(patient, "Observation/" + observation.getIdElement().getIdPart(),
                findColonoscopy(patient));
    }

    private String findColonoscopy(String patient) {
        Bundle bundle = client.search()
                .forResource(Procedure.class)
                .where(Procedure.SUBJECT.hasId(patient))
                .and(Procedure.CODE.exactly().code("73761001"))
                .count(1)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().isEmpty()
                ? null
                : "Procedure/" + bundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
    }
}
