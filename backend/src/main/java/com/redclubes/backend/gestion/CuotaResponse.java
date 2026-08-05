package com.redclubes.backend.gestion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(
        Long id,
        Long clubId,
        Long socioId,
        String socioNombre,
        String socioDni,
        String mes,
        String periodo,
        BigDecimal importe,
        EstadoCuota estado,
        LocalDate fechaEmision,
        LocalDate vencimiento
) {
    public static CuotaResponse desde(Cuota cuota) {
        return new CuotaResponse(
                cuota.getId(),
                cuota.getClub().getId(),
                cuota.getSocio().getId(),
                cuota.getSocio().getNombre() + " " + cuota.getSocio().getApellido(),
                cuota.getSocio().getDni(),
                cuota.getPeriodo(),
                cuota.getPeriodo(),
                cuota.getImporte(),
                cuota.getEstado(),
                cuota.getFechaEmision(),
                cuota.getVencimiento()
        );
    }
}
