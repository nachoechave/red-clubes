package com.redclubes.backend.gestion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearActividadRequest(
        @NotBlank String nombre,
        @NotNull Long profesorUsuarioId,
        String profesor,
        @NotBlank String dias,
        @NotBlank String categoria,
        String icono,
        @Min(1) int cupo
) {
}
