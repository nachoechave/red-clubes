package com.redclubes.backend.gestion;

public record ImportacionDeudaHistoricaError(
        int fila,
        String dni,
        String mensaje
) {
}
