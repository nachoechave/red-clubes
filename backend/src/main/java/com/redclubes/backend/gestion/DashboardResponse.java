package com.redclubes.backend.gestion;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        int sociosActivos,
        int sociosTotales,
        int cuotasAlDia,
        int morosos,
        int actividadesActivas,
        int asistenciaMes,
        BigDecimal totalCobrado,
        List<MovimientoResponse> movimientos,
        List<ActividadResponse> proximasActividades,
        List<CuotaResponse> cuotasPendientes,
        List<SocioResumenResponse> sociosRecientes,
        List<GraficoMesResponse> pagosPorMes,
        List<GraficoMesResponse> asistenciaMensual
) {
}
