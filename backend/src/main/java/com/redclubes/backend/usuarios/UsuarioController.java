package com.redclubes.backend.usuarios;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public UsuarioController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @GetMapping
    public List<UsuarioResponse> listarUsuarios(@RequestHeader("Authorization") String authorizationHeader) {
        Usuario usuarioAutenticado = authService.exigirAdministrador(authorizationHeader);
        return usuarioService.listarUsuarios(usuarioAutenticado);
    }

    @PostMapping
    public UsuarioResponse crearUsuario(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CrearUsuarioRequest request
    ) {
        Usuario usuarioAutenticado = authService.exigirAdministrador(authorizationHeader);
        if (request.rol() == RolUsuario.SUPERUSUARIO && usuarioAutenticado.getRol() != RolUsuario.SUPERUSUARIO) {
            throw new AccesoDenegadoException();
        }

        return usuarioService.crearUsuario(request, usuarioAutenticado);
    }

    @DeleteMapping("/{id}")
    public UsuarioResponse desactivarUsuario(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {
        Usuario usuarioAutenticado = authService.exigirAdministrador(authorizationHeader);
        return usuarioService.desactivarUsuario(id, usuarioAutenticado);
    }

    @PutMapping("/{id}/activar")
    public UsuarioResponse activarUsuario(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {
        Usuario usuarioAutenticado = authService.exigirAdministrador(authorizationHeader);
        if (usuarioAutenticado.getRol() != RolUsuario.SUPERUSUARIO) {
            throw new AccesoDenegadoException();
        }

        return usuarioService.activarUsuario(id, usuarioAutenticado);
    }

    @PutMapping("/{id}/asignaciones")
    public UsuarioResponse actualizarAsignaciones(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestBody ActualizarAsignacionesUsuarioRequest request
    ) {
        Usuario usuarioAutenticado = authService.exigirAdministrador(authorizationHeader);
        return usuarioService.actualizarAsignaciones(id, request, usuarioAutenticado);
    }
}
