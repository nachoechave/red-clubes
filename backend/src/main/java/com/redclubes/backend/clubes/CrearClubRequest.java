package com.redclubes.backend.clubes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearClubRequest(
        @NotBlank(message = "El nombre del club es obligatorio")
        @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
        String nombre,

        @NotBlank(message = "La direccion es obligatoria")
        @Size(min = 2, max = 120, message = "La direccion debe tener entre 2 y 120 caracteres")
        String direccion,

        @Size(max = 1_500_000, message = "El logo es demasiado grande")
        String logoUrl
) {
}
