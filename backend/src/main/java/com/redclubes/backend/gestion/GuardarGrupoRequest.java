package com.redclubes.backend.gestion;

import jakarta.validation.constraints.*;

public record GuardarGrupoRequest(
        @NotBlank @Size(max = 255) String nombre,
        @NotNull TipoGrupo tipo,
        @Size(max = 255) String disciplina,
        @Size(max = 255) String anioCategoria,
        Long profesorUsuarioId,
        @Size(max = 255) String dias,
        @Size(max = 255) String categoria,
        @Size(max = 255) String icono,
        @Min(1) Integer cupo,
        @NotNull Boolean asistenciaHabilitada,
        EstadoActividad estado
) {}
