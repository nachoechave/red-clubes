package com.redclubes.backend.usuarios;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El DNI es obligatorio")
        String dni,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}
