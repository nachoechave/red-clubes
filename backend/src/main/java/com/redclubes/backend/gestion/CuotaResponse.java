package com.redclubes.backend.gestion;

import java.time.LocalDate;

public record CuotaResponse(
        Long id,
        Long clubId,
        Long socioId,
        String socioNombre,
        String socioDni,
        String mes,
        int importe,
        EstadoCuota estado,
        LocalDate vencimiento
) {
    public static CuotaResponse desde(Cuota cuota) {
        return new CuotaResponse(
                cuota.getId(),
                cuota.getClub().getId(),
                cuota.getSocio().getId(),
                cuota.getSocio().getNombre() + " " + cuota.getSocio().getApellido(),
                cuota.getSocio().getDni(),
                cuota.getMes(),
                cuota.getImporte(),
                cuota.getEstado(),
                cuota.getVencimiento()
        );
    }
}
