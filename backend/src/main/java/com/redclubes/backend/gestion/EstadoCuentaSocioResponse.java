package com.redclubes.backend.gestion;

import java.math.BigDecimal;

public record EstadoCuentaSocioResponse(
        Long socioId,
        Integer numeroSocio,
        String socioNombre,
        String socioDni,
        String estadoSocio,
        int cuotasPendientes,
        int cuotasVencidas,
        int cuotasAdeudadas,
        BigDecimal deudaTotal,
        String ultimoPeriodo,
        boolean alDia
) {
}
