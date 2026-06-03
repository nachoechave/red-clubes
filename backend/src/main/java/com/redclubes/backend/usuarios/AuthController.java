package com.redclubes.backend.usuarios;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UsuarioResponse me(@RequestHeader("Authorization") String authorizationHeader) {
        return authService.me(authorizationHeader);
    }

    @PostMapping("/cambiar-password")
    public UsuarioResponse cambiarPassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CambiarPasswordRequest request
    ) {
        return authService.cambiarPassword(authorizationHeader, request);
    }

    @PutMapping("/me")
    public UsuarioResponse actualizarPerfil(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ActualizarPerfilRequest request
    ) {
        return authService.actualizarPerfil(authorizationHeader, request);
    }
}
