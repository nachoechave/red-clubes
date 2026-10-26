package com.redclubes.backend.gestion;

import java.util.List;

public record ImportacionDeudaHistoricaResponse(
        boolean valida,
        int totalFilas,
        int importadas,
        List<ImportacionDeudaHistoricaItemResponse> cuotasImportadas,
        List<ImportacionDeudaHistoricaError> errores
) {
}
