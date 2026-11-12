package io.github.screeningloop.chat;

import io.github.screeningloop.patient.PatientSummary;
import io.github.screeningloop.patient.PatientSummaryService;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final PatientSummaryService summaries;
    private final IntentClassifier classifier = new IntentClassifier();
    private final ChatComposer composer;

    public ChatService(PatientSummaryService summaries, Clock clock) {
        this.summaries = summaries;
        this.composer = new ChatComposer(clock);
    }

    public ChatReply answer(String patientId, String text) {
        Intent intent = classifier.classify(text);
        PatientSummary summary = intent.needsPatientData()
                ? summaries.summarize(patientId, "Patient/" + patientId)
                : null;
        return new ChatReply(composer.compose(intent, summary), intent.name());
    }
}
