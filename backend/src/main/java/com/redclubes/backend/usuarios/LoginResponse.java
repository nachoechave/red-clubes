package com.redclubes.backend.usuarios;

public record LoginResponse(
        String token,
        UsuarioResponse usuario
) {
}
