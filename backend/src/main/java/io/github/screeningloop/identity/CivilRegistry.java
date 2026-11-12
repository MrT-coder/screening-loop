package io.github.screeningloop.identity;

import java.time.LocalDate;
import java.util.Optional;

public interface CivilRegistry {

    Optional<RegistryRecord> find(String documentNumber);

    record RegistryRecord(String patientId, LocalDate birthDate) {
    }
}
