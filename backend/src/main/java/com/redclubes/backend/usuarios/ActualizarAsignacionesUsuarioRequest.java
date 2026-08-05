package com.redclubes.backend.usuarios;

import java.util.List;

public record ActualizarAsignacionesUsuarioRequest(
        List<AsignacionUsuarioRequest> asignaciones
) {
}
