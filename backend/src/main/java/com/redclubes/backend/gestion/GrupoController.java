package com.redclubes.backend.gestion;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

/** API genérica sobre las mismas identidades y reglas que las rutas históricas. */
@RestController
@RequestMapping("/api/clubes/{clubId}")
public class GrupoController {
    private final GestionService gestion;
    private final AuthService auth;
    public GrupoController(GestionService gestion, AuthService auth) {
        this.gestion = gestion;
        this.auth = auth;
    }

    @GetMapping({"/grupos", "/reportes/grupos"})
    public List<ActividadResponse> listar(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @RequestParam(required = false) TipoGrupo tipo) {
        auth.exigirAccesoAClub(authorization, clubId);
        Usuario usuario = auth.obtenerUsuarioAutenticado(authorization);
        var grupos = auth.puedeOperarClub(usuario, clubId) ? gestion.listarActividades(clubId)
                : gestion.listarActividades(clubId, auth.actividadesPermitidas(usuario, clubId));
        return grupos.stream().filter(grupo -> tipo == null || grupo.tipo() == tipo).toList();
    }

    @PostMapping("/grupos")
    public ActividadResponse crear(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @Valid @RequestBody GuardarGrupoRequest request) {
        auth.exigirAdministradorDeClub(authorization, clubId);
        return gestion.guardarGrupo(clubId, null, request, auth.obtenerUsuarioAutenticado(authorization));
    }

    @PutMapping("/grupos/{grupoId}")
    public ActividadResponse actualizar(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @PathVariable Long grupoId, @Valid @RequestBody GuardarGrupoRequest request) {
        auth.exigirAdministradorDeClub(authorization, clubId);
        return gestion.guardarGrupo(clubId, grupoId, request, auth.obtenerUsuarioAutenticado(authorization));
    }

    public record InscripcionesRequest(@NotNull List<@NotNull Long> grupoIds, String observaciones) {}

    @GetMapping("/socios/{socioId}/grupos")
    public List<ActividadResponse> inscripciones(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @PathVariable Long socioId) {
        auth.exigirOperadorDeClub(authorization, clubId);
        return gestion.listarActividadesDeSocio(clubId, socioId);
    }

    @PutMapping("/socios/{socioId}/grupos")
    public List<ActividadResponse> inscribir(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @PathVariable Long socioId, @Valid @RequestBody InscripcionesRequest request) {
        auth.exigirOperadorDeClub(authorization, clubId);
        return gestion.actualizarActividadesDeSocio(clubId, socioId,
                new ActualizarInscripcionesRequest(request.grupoIds(), request.observaciones()), auth.obtenerUsuarioAutenticado(authorization));
    }

    @GetMapping("/grupos/{grupoId}/asistencias/{fecha}")
    public List<AsistenciaResponse> asistencia(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @PathVariable Long grupoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        autorizarAsistencia(authorization, clubId, grupoId);
        return gestion.listarAsistencia(clubId, grupoId, fecha);
    }

    @PostMapping("/grupos/{grupoId}/asistencias/{fecha}")
    public List<AsistenciaResponse> guardarAsistencia(@RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId, @PathVariable Long grupoId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Valid @RequestBody List<@Valid GuardarAsistenciaRequest> request) {
        Usuario usuario = autorizarAsistencia(authorization, clubId, grupoId);
        return gestion.guardarAsistencia(clubId, grupoId, fecha, request, !auth.puedeOperarClub(usuario, clubId), usuario);
    }

    private Usuario autorizarAsistencia(String authorization, Long clubId, Long grupoId) {
        auth.exigirAccesoAClub(authorization, clubId);
        Usuario usuario = auth.obtenerUsuarioAutenticado(authorization);
        auth.exigirAccesoAActividad(usuario, clubId, grupoId);
        return usuario;
    }
}
