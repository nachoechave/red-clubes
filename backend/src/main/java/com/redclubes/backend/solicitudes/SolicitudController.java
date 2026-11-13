package com.redclubes.backend.solicitudes;

import com.redclubes.backend.usuarios.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/clubes/{clubId}/solicitudes")
public class SolicitudController {
    private final SolicitudService servicio;
    private final AuthService auth;
    public SolicitudController(SolicitudService servicio, AuthService auth) { this.servicio = servicio; this.auth = auth; }
    @GetMapping
    public SolicitudService.Pagina listar(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long clubId, @RequestParam(defaultValue = "PENDIENTE") SolicitudInscripcion.Estado estado,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int pagina) {
        auth.exigirAdministradorDeClub(authorization, clubId);
        return servicio.listar(clubId, estado, pagina);
    }
    @PostMapping("/{id}/aprobar")
    public SolicitudResponse aprobar(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long clubId, @PathVariable Long id, @Valid @RequestBody DatosSolicitud datos) {
        auth.exigirAdministradorDeClub(authorization, clubId);
        return servicio.aprobar(clubId, id, datos, auth.obtenerUsuarioAutenticado(authorization));
    }
    public record Rechazo(@NotBlank @Size(max = 500) String motivo) {}
    @PostMapping("/{id}/rechazar")
    public SolicitudResponse rechazar(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long clubId, @PathVariable Long id, @Valid @RequestBody Rechazo rechazo) {
        auth.exigirAdministradorDeClub(authorization, clubId);
        return servicio.rechazar(clubId, id, rechazo.motivo(), auth.obtenerUsuarioAutenticado(authorization));
    }
}
