package io.github.screeningloop.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.screeningloop.cases.CaseCodes;
import io.github.screeningloop.cases.CaseTaskMapper;
import io.github.screeningloop.cases.CaseTransitionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;

class CaseDetailTest {

    private static final String ACTOR = "Practitioner/gp-1";
    private static final String DECIDER = "Practitioner/gp-2";
    private static final Instant FIRST = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-11-03T09:30:00Z");

    private Task task() {
        Task task = new CaseTaskMapper(new CaseTransitionPolicy()).openCase(
                "Patient/p1", "Observation/fit-1", ACTOR, LocalDate.parse("2026-12-01"), Instant.EPOCH);
        task.setId("t1");
        return task;
    }

    private Provenance provenance(String stage, Instant recorded, String... agents) {
        Provenance provenance = new Provenance();
        provenance.setRecorded(Date.from(recorded));
        provenance.setActivity(new CodeableConcept(new Coding(CaseCodes.STAGE_SYSTEM, stage, null)));
        for (String agent : agents) {
            provenance.addAgent().setWho(new Reference(agent));
        }
        return provenance;
    }

    @Test
    void listsTheHistoryInChronologicalOrder() {
        List<Provenance> unordered = List.of(
                provenance("acknowledged", SECOND, ACTOR),
                provenance("patient-notified", FIRST, ACTOR));
        CaseDetail detail = CaseDetail.from(task(), unordered);
        assertThat(detail.history()).extracting(HistoryEntry::stage)
                .containsExactly("patient-notified", "acknowledged");
    }

    @Test
    void exposesTheSummaryAndTheAgentsOfEachEntry() {
        CaseDetail detail = CaseDetail.from(task(), List.of(
                provenance("closed-without-care", FIRST, ACTOR, DECIDER)));
        assertThat(detail.summary().id()).isEqualTo("t1");
        assertThat(detail.history().get(0).agents()).containsExactly(ACTOR, DECIDER);
        assertThat(detail.history().get(0).recordedAt()).isEqualTo("2026-11-01T10:00:00Z");
    }

    @Test
    void returnsAnEmptyHistoryWhenTheCaseWasNeverAdvanced() {
        assertThat(CaseDetail.from(task(), List.of()).history()).isEmpty();
    }
}
