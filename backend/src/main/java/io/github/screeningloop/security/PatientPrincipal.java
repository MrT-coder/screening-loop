package io.github.screeningloop.security;

import java.io.Serializable;

public record PatientPrincipal(String patientId) implements Serializable {
}
