package com.redclubes.backend.usuarios;

import java.util.List;

public record AsignacionUsuarioRequest(
        Long clubId,
        RolClub rolClub,
        List<Long> actividadIds
) {
}
