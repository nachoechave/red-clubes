package com.redclubes.backend.gestion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GenerarCuotasRequest(
        @NotBlank(message = "El periodo es obligatorio")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El periodo debe tener formato YYYY-MM")
        String periodo,
        @NotNull(message = "El importe es obligatorio")
        @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
        BigDecimal importe,
        @NotNull(message = "El vencimiento es obligatorio") LocalDate vencimiento
) {
}
