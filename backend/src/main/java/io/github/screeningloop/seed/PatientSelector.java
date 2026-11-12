package io.github.screeningloop.seed;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PatientSelector {

    private final SyntheaBundleReader reader;
    private final FitResultClassifier classifier;

    public PatientSelector(SyntheaBundleReader reader, FitResultClassifier classifier) {
        this.reader = reader;
        this.classifier = classifier;
    }

    public PatientSelection select(List<Path> patientFiles, int backgroundCount) {
        List<Path> positives = new ArrayList<>();
        List<Path> background = new ArrayList<>();
        for (Path file : patientFiles) {
            if (classifier.hasPositiveResult(reader.read(file))) {
                positives.add(file);
            } else if (background.size() < backgroundCount) {
                background.add(file);
            }
        }
        return new PatientSelection(positives, background);
    }
}
