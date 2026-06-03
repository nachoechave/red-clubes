package com.redclubes.backend.usuarios;

public record ClubAsignadoResponse(
        Long clubId,
        String clubNombre,
        RolClub rol
) {
    public static ClubAsignadoResponse desde(UsuarioClub usuarioClub) {
        return new ClubAsignadoResponse(
                usuarioClub.getClub().getId(),
                usuarioClub.getClub().getNombre(),
                usuarioClub.getRol()
        );
    }
}
