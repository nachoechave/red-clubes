package com.redclubes.backend.gestion;

public record ImportacionDeudaHistoricaItemResponse(
        int fila,
        String dni,
        String periodo,
        Long cuotaId
) {
}
