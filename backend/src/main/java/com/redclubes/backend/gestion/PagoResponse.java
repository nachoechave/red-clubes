package com.redclubes.backend.gestion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long cuotaId,
        BigDecimal importe,
        LocalDateTime fechaPago,
        MedioPago medioPago,
        Long usuarioResponsableId,
        String observaciones,
        EstadoPago estado,
        LocalDateTime fechaAnulacion,
        Long usuarioAnulacionId
) {
    public static PagoResponse desde(Pago pago) {
        return new PagoResponse(
                pago.getId(), pago.getCuota().getId(), pago.getImporte(), pago.getFechaPago(), pago.getMedioPago(),
                pago.getUsuarioResponsable() == null ? null : pago.getUsuarioResponsable().getId(),
                pago.getObservaciones(), pago.getEstado(), pago.getFechaAnulacion(),
                pago.getUsuarioAnulacion() == null ? null : pago.getUsuarioAnulacion().getId()
        );
    }
}
