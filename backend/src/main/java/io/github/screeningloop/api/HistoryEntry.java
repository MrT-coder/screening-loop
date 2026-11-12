package io.github.screeningloop.api;

import java.util.List;
import org.hl7.fhir.r4.model.Provenance;

public record HistoryEntry(String recordedAt, String stage, List<String> agents) {

    public static HistoryEntry from(Provenance provenance) {
        return new HistoryEntry(
                provenance.getRecorded().toInstant().toString(),
                provenance.getActivity().getCodingFirstRep().getCode(),
                provenance.getAgent().stream()
                        .map(agent -> agent.getWho().getReference())
                        .toList());
    }
}
