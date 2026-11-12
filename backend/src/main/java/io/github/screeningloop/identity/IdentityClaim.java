package io.github.screeningloop.identity;

import java.time.LocalDate;

public record IdentityClaim(String documentNumber, LocalDate birthDate, String livenessToken) {
}
