package com.redclubes.backend.clubes;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ConfiguracionClubRequest(
        @NotNull Set<@NotNull ModuloClub> modulos,
        @NotNull NombreParticipantes nombreParticipantes
) {}
