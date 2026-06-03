package com.redclubes.backend.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 80, message = "La nueva contrasena debe tener entre 8 y 80 caracteres")
        String nuevaPassword
) {
}
