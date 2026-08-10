package com.redclubes.backend.socios;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.Usuario;

@RestController
public class SocioController {

    private final SocioService socioService;
    private final AuthService authService;

    public SocioController(SocioService socioService, AuthService authService) {
        this.socioService = socioService;
        this.authService = authService;
    }

    @GetMapping("/api/clubes/{clubId}/socios")
    public List<SocioResponse> listarSociosPorClub(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return socioService.listarSociosPorClub(clubId);
    }

    @PostMapping("/api/clubes/{clubId}/socios")
    public SocioResponse crearSocioEnClub(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long clubId,
            @Valid @RequestBody CrearSocioRequest request
    ) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return socioService.crearSocioEnClub(clubId, request, actor);
    }

    @PutMapping("/api/clubes/{clubId}/socios/{id}")
    public SocioResponse actualizarSocioEnClub(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarSocioRequest request
    ) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return socioService.actualizarSocioEnClub(clubId, id, request, actor);
    }

    @DeleteMapping("/api/clubes/{clubId}/socios/{id}")
    public SocioResponse eliminarSocioEnClub(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long id
    ) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return socioService.eliminarSocioEnClub(clubId, id, actor);
    }
}
