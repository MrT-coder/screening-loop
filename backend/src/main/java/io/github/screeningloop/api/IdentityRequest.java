package io.github.screeningloop.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IdentityRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9-]+") String documentNumber,
        @NotNull LocalDate birthDate,
        @NotBlank @Size(max = 64) String livenessToken) {
}
