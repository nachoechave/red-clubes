package com.redclubes.backend.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrearUsuarioRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Size(min = 7, max = 10, message = "El DNI debe tener entre 7 y 10 caracteres")
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        String apellido,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,

        Long clubId,

        RolClub rolClub,

        @NotBlank(message = "La contrasena inicial es obligatoria")
        @Size(min = 6, max = 80, message = "La contrasena debe tener entre 6 y 80 caracteres")
        String passwordInicial,

        List<AsignacionUsuarioRequest> asignaciones
) {
}
