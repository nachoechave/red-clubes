package com.redclubes.backend.socios;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import com.redclubes.backend.usuarios.AuthService;

@RestController
public class SocioController {

    private final SocioService socioService;
    private final AuthService authService;

    public SocioController(SocioService socioService, AuthService authService) {
        this.socioService = socioService;
        this.authService = authService;
    }

    @GetMapping("/api/socios")
    public List<Socio> listarSocios() {
        return socioService.listarSocios();
    }

    @GetMapping("/api/clubes/{clubId}/socios")
    public List<Socio> listarSociosPorClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return socioService.listarSociosPorClub(clubId);
    }

    @PostMapping("/api/socios")
    public Socio crearSocio(@Valid @RequestBody Socio socio) {
        return socioService.crearSocio(socio);
    }

    @PostMapping("/api/clubes/{clubId}/socios")
    public Socio crearSocioEnClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @Valid @RequestBody Socio socio
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return socioService.crearSocioEnClub(clubId, socio);
    }

    @GetMapping("/api/socios/{id}")
    public Socio obtenerSocioPorId(@PathVariable Long id) {
        return socioService.obtenerSocioPorId(id);
    }

    @PutMapping("/api/socios/{id}")
    public Socio actualizarSocio(
        @PathVariable Long id,
        @Valid @RequestBody Socio socioActualizado
    ) {
        return socioService.actualizarSocio(id, socioActualizado);
    }   

    @PutMapping("/api/clubes/{clubId}/socios/{id}")
    public Socio actualizarSocioEnClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long id,
            @Valid @RequestBody Socio socioActualizado
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return socioService.actualizarSocio(id, socioActualizado);
    }

    @DeleteMapping("/api/socios/{id}")
    public Socio eliminarSocio(@PathVariable Long id) {
        return socioService.eliminarSocio(id);
    }

    @DeleteMapping("/api/clubes/{clubId}/socios/{id}")
    public Socio eliminarSocioEnClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long id
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return socioService.eliminarSocio(id);
    }
}
