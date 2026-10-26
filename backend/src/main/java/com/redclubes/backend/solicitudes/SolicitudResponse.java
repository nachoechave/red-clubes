package com.redclubes.backend.solicitudes;

import java.time.LocalDateTime;

public record SolicitudResponse(Long id, String referencia, DatosSolicitud datos,
        SolicitudInscripcion.Estado estado, LocalDateTime fechaCreacion, LocalDateTime fechaRevision,
        Long usuarioRevisionId, Long socioId, String motivo) {
    static SolicitudResponse desde(SolicitudInscripcion s) {
        return new SolicitudResponse(s.id, s.referencia,
                new DatosSolicitud(s.nombre, s.apellido, s.dni, s.email, s.telefono),
                s.estado, s.fechaCreacion, s.fechaRevision, s.usuarioRevisionId, s.socioId, s.motivo);
    }
}
