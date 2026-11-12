package io.github.screeningloop.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OpenCaseRequest(
        @NotBlank String patient,
        @NotBlank String fitResult,
        @NotBlank String owner,
        @NotNull LocalDate dueDate) {
}
