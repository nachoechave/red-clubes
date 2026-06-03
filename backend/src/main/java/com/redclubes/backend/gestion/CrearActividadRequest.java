package com.redclubes.backend.gestion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrearActividadRequest(
        @NotBlank String nombre,
        @NotBlank String profesor,
        @NotBlank String dias,
        @NotBlank String categoria,
        String icono,
        @Min(1) int cupo
) {
}
