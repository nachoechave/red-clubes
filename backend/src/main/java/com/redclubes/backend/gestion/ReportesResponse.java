package com.redclubes.backend.gestion;

import java.math.BigDecimal;
import java.util.List;

public record ReportesResponse(
        List<ResumenClubResponse> sociosPorClub,
        List<GraficoMesResponse> pagosPorMes,
        List<GraficoMesResponse> asistenciaMensual,
        BigDecimal deudaPendiente,
        BigDecimal deudaVencida,
        long cuotasConDeuda,
        long sociosConDeuda
) {
}
