package com.redclubes.backend.socios;

public record ImportacionSociosError(
        int fila,
        String dni,
        String mensaje
) {
}
