package com.redclubes.backend.solicitudes;

import jakarta.validation.constraints.*;

public record DatosSolicitud(
        @NotBlank @Size(min = 2, max = 50) String nombre,
        @NotBlank @Size(min = 2, max = 50) String apellido,
        @NotBlank @Pattern(regexp = "\\d{7,10}") String dni,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 30) @Pattern(regexp = "[+0-9() .-]+") String telefono
) {
    public DatosSolicitud {
        nombre = limpiar(nombre); apellido = limpiar(apellido); dni = limpiar(dni);
        email = limpiar(email); telefono = limpiar(telefono);
    }
    private static String limpiar(String valor) { return valor == null ? null : valor.trim(); }
}
