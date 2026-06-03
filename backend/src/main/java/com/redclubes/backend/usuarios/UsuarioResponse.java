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
        return desde(usuario, List.of());
    }

    public static UsuarioResponse desde(Usuario usuario, List<UsuarioClub> clubes) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getDni(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.isDebeCambiarPassword(),
                clubes.stream().map(ClubAsignadoResponse::desde).toList()
        );
    }
}
