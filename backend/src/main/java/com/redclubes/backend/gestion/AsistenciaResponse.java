package com.redclubes.backend.gestion;

import java.time.LocalDate;

public record AsistenciaResponse(
        Long id,
        Long actividadId,
        Long socioId,
        String socioNombre,
        LocalDate fecha,
        boolean presente
) {
    public static AsistenciaResponse desde(Asistencia asistencia) {
        return new AsistenciaResponse(
                asistencia.getId(),
                asistencia.getActividad().getId(),
                asistencia.getSocio().getId(),
                asistencia.getSocio().getNombre() + " " + asistencia.getSocio().getApellido(),
                asistencia.getFecha(),
                asistencia.isPresente()
        );
    }
}
