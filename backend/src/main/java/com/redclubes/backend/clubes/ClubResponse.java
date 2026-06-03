package com.redclubes.backend.clubes;

public record ClubResponse(
        Long id,
        String nombre,
        String direccion,
        EstadoClub estado
) {
    public static ClubResponse desde(Club club) {
        return new ClubResponse(
                club.getId(),
                club.getNombre(),
                club.getDireccion(),
                club.getEstado()
        );
    }
}
