package com.redclubes.backend.gestion;

import jakarta.validation.constraints.NotNull;

public record GuardarAsistenciaRequest(
        @NotNull(message = "El socio es obligatorio") Long socioId,
        Boolean presente,
        EstadoAsistencia estado
) {
    public EstadoAsistencia estadoEfectivo() {
        if (estado != null) {
            return estado;
        }
        if (presente == null) {
            throw new IllegalArgumentException("Debe indicar el estado de asistencia");
        }
        return presente ? EstadoAsistencia.PRESENTE : EstadoAsistencia.AUSENTE;
    }
}
