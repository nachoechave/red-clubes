package com.redclubes.backend.gestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularPagoRequest(
        @NotBlank(message = "El motivo de anulacion es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
