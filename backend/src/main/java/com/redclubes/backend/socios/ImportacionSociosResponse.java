package com.redclubes.backend.socios;

import java.util.List;

public record ImportacionSociosResponse(
        boolean valida,
        int totalFilas,
        int importados,
        List<ImportacionSociosItemResponse> sociosImportados,
        List<ImportacionSociosError> errores
) {
}
