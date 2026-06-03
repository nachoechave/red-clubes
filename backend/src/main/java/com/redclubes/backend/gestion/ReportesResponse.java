package com.redclubes.backend.gestion;

import java.util.List;

public record ReportesResponse(
        List<ResumenClubResponse> sociosPorClub,
        List<GraficoMesResponse> pagosPorMes,
        List<GraficoMesResponse> asistenciaMensual
) {
}
