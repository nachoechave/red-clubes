package com.redclubes.backend.socios;

import java.time.LocalDate;

public record SocioResponse(
        Long id,
        Long clubId,
        String nombre,
        String apellido,
        String dni,
        String email,
        LocalDate fechaNacimiento,
        LocalDate fechaAlta,
        Integer numeroSocio,
        String telefono,
        String direccion,
        String emergenciaNombre,
        String emergenciaTelefono,
        String emergenciaRelacion,
        String estado
) {
    public static SocioResponse from(Socio socio) {
        return new SocioResponse(
                socio.getId(),
                socio.getClub().getId(),
                socio.getNombre(),
                socio.getApellido(),
                socio.getDni(),
                socio.getEmail(),
                socio.getFechaNacimiento(),
                socio.getFechaAlta(),
                socio.getNumeroSocio(),
                socio.getTelefono(),
                socio.getDireccion(),
                socio.getEmergenciaNombre(),
                socio.getEmergenciaTelefono(),
                socio.getEmergenciaRelacion(),
                socio.getEstado()
        );
    }
}
