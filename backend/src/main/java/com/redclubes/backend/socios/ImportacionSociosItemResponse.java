package com.redclubes.backend.socios;

public record ImportacionSociosItemResponse(
        int fila,
        String dni,
        Long socioId,
        Integer numeroSocio
) {
}
