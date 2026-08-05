package com.redclubes.backend.usuarios;

import java.util.List;

public record UsuarioResponse(
        Long id,
        String dni,
        String nombre,
        String apellido,
        RolUsuario rol,
        EstadoUsuario estado,
        boolean debeCambiarPassword,
        List<ClubAsignadoResponse> clubes
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return desde(usuario, List.of(), List.of());
    }

    public static UsuarioResponse desde(Usuario usuario, List<UsuarioClub> clubes) {
        return desde(usuario, clubes, List.of());
    }

    public static UsuarioResponse desde(Usuario usuario, List<UsuarioClub> clubes, List<UsuarioActividad> actividades) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getDni(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.isDebeCambiarPassword(),
                clubes.stream().map(club -> ClubAsignadoResponse.desde(club, actividades)).toList()
        );
    }
}
