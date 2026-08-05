package com.redclubes.backend.auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        Long clubId,
        String accion,
        String tipoEntidad,
        Long entidadId,
        LocalDateTime fecha,
        String detalle
) {
    public static AuditoriaResponse desde(Auditoria auditoria) {
        return new AuditoriaResponse(
                auditoria.getId(), auditoria.getUsuario().getId(),
                auditoria.getUsuario().getNombre() + " " + auditoria.getUsuario().getApellido(),
                auditoria.getClub() == null ? null : auditoria.getClub().getId(),
                auditoria.getAccion(), auditoria.getTipoEntidad(), auditoria.getEntidadId(),
                auditoria.getFecha(), auditoria.getDetalle()
        );
    }
}
