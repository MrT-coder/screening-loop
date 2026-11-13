package io.github.screeningloop.chat;

import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import org.springframework.stereotype.Service;

@Service
public class ClinicianChatService {

    private final PatientSummaryService summaries;

    public ClinicianChatService(PatientSummaryService summaries) {
        this.summaries = summaries;
    }

    public ChatReply answer(String patientId, String agent, String text) {
        ClinicianIntent intent = ClinicianIntent.classify(text);
        PatientSummary summary = intent.needsPatientData() ? summaries.summarize(patientId, agent) : null;
        return new ChatReply(ClinicianReplies.compose(intent, summary), intent.name());
    }
}
