package com.redclubes.backend.gestion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarPagoRequest(
        @NotNull(message = "El medio de pago es obligatorio") MedioPago medioPago,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres") String observaciones
) {
}
