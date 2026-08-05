package com.redclubes.backend.clubes;

public record ClubResponse(
        Long id,
        String nombre,
        String direccion,
        String logoUrl,
        EstadoClub estado
) {
    public static ClubResponse desde(Club club) {
        return new ClubResponse(
                club.getId(),
                club.getNombre(),
                club.getDireccion(),
                club.getLogoUrl(),
                club.getEstado()
        );
    }
}
