package io.github.screeningloop.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(@NotBlank @Size(max = 300) String text) {
}
