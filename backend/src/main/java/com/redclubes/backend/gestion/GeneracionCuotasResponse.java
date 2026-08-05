package com.redclubes.backend.gestion;

import java.util.List;

public record GeneracionCuotasResponse(
        String periodo,
        int creadas,
        int omitidas,
        List<CuotaResponse> cuotas
) {
}
