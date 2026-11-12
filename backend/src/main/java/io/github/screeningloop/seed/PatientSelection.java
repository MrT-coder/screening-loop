package io.github.screeningloop.seed;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record PatientSelection(List<Path> positives, List<Path> background) {

    public List<Path> all() {
        List<Path> files = new ArrayList<>(positives);
        files.addAll(background);
        return files;
    }
}
