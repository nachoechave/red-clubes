package com.redclubes.backend.usuarios;

import java.util.List;

public record ClubAsignadoResponse(
        Long clubId,
        String clubNombre,
        RolClub rol,
        List<Long> actividadIds
) {
    public static ClubAsignadoResponse desde(UsuarioClub usuarioClub, List<UsuarioActividad> actividades) {
        return new ClubAsignadoResponse(
                usuarioClub.getClub().getId(),
                usuarioClub.getClub().getNombre(),
                usuarioClub.getRol(),
                actividades.stream()
                        .filter(usuarioActividad -> usuarioActividad.getClub().getId().equals(usuarioClub.getClub().getId()))
                        .map(usuarioActividad -> usuarioActividad.getActividad().getId())
                        .toList()
        );
    }
}
