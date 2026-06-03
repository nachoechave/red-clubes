package com.redclubes.backend.gestion;

import java.util.List;

public record DashboardResponse(
        int sociosActivos,
        int sociosTotales,
        int cuotasAlDia,
        int morosos,
        int actividadesActivas,
        int asistenciaMes,
        int totalCobrado,
        List<MovimientoResponse> movimientos,
        List<ActividadResponse> proximasActividades,
        List<CuotaResponse> cuotasPendientes,
        List<SocioResumenResponse> sociosRecientes,
        List<GraficoMesResponse> pagosPorMes,
        List<GraficoMesResponse> asistenciaMensual
) {
}
