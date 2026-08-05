package com.redclubes.backend.auditoria;

import com.redclubes.backend.usuarios.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clubes/{clubId}/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;
    private final AuthService authService;

    public AuditoriaController(AuditoriaService auditoriaService, AuthService authService) {
        this.auditoriaService = auditoriaService;
        this.authService = authService;
    }

    @GetMapping
    public List<AuditoriaResponse> listar(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId
    ) {
        authService.exigirAdministradorDeClub(authorization, clubId);
        return auditoriaService.listar(clubId);
    }
}
