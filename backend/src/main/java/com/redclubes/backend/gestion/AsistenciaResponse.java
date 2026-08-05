package com.redclubes.backend.gestion;

import com.redclubes.backend.socios.Socio;

import java.time.LocalDate;

public record AsistenciaResponse(
        Long id,
        Long actividadId,
        Long socioId,
        String socioNombre,
        String socioDni,
        String socioTelefono,
        LocalDate fecha,
        boolean presente,
        EstadoAsistencia estado,
        Long usuarioResponsableId
) {
    public static AsistenciaResponse desde(Asistencia asistencia) {
        return new AsistenciaResponse(
                asistencia.getId(),
                asistencia.getActividad().getId(),
                asistencia.getSocio().getId(),
                asistencia.getSocio().getNombre() + " " + asistencia.getSocio().getApellido(),
                asistencia.getSocio().getDni(),
                asistencia.getSocio().getTelefono(),
                asistencia.getFecha(),
                asistencia.isPresente(),
                asistencia.getEstado(),
                asistencia.getUsuarioResponsable() == null ? null : asistencia.getUsuarioResponsable().getId()
        );
    }

    public static AsistenciaResponse sinRegistro(Long actividadId, Socio socio, LocalDate fecha) {
        return new AsistenciaResponse(
                null,
                actividadId,
                socio.getId(),
                socio.getNombre() + " " + socio.getApellido(),
                socio.getDni(),
                socio.getTelefono(),
                fecha,
                false,
                EstadoAsistencia.AUSENTE,
                null
        );
    }
}
