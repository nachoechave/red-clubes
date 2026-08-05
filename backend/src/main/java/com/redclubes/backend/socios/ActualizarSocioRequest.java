package com.redclubes.backend.socios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ActualizarSocioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        String apellido,
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{7,10}", message = "El DNI debe contener entre 7 y 10 digitos")
        String dni,
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO")
        String estado,
        @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
        String telefono,
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 120, message = "El email no puede superar 120 caracteres")
        String email,
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,
        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        String direccion,
        @Size(max = 100, message = "El contacto de emergencia no puede superar 100 caracteres")
        String emergenciaNombre,
        @Size(max = 30, message = "El telefono de emergencia no puede superar 30 caracteres")
        String emergenciaTelefono,
        @Size(max = 50, message = "La relacion de emergencia no puede superar 50 caracteres")
        String emergenciaRelacion
) {
}
