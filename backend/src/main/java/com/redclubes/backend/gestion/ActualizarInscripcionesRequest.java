package com.redclubes.backend.gestion;

import java.util.List;

public record ActualizarInscripcionesRequest(List<Long> actividadIds, String observaciones) {
}
