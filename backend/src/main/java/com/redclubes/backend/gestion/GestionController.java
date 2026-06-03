package com.redclubes.backend.gestion;

import com.redclubes.backend.usuarios.AuthService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clubes/{clubId}")
public class GestionController {

    private final GestionService gestionService;
    private final AuthService authService;

    public GestionController(GestionService gestionService, AuthService authService) {
        this.gestionService = gestionService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return gestionService.dashboard(clubId);
    }

    @GetMapping("/actividades")
    public List<ActividadResponse> actividades(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return gestionService.listarActividades(clubId);
    }

    @PostMapping("/actividades")
    public ActividadResponse crearActividad(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @Valid @RequestBody CrearActividadRequest request
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return gestionService.crearActividad(clubId, request);
    }

    @GetMapping("/cuotas")
    public List<CuotaResponse> cuotas(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return gestionService.listarCuotas(clubId);
    }

    @PostMapping("/cuotas/{cuotaId}/pago")
    public CuotaResponse registrarPago(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long cuotaId
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return gestionService.registrarPago(cuotaId);
    }

    @GetMapping("/actividades/{actividadId}/asistencias/{fecha}")
    public List<AsistenciaResponse> asistencias(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long actividadId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return gestionService.listarAsistencia(clubId, actividadId, fecha);
    }

    @PostMapping("/actividades/{actividadId}/asistencias/{fecha}")
    public List<AsistenciaResponse> guardarAsistencia(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long actividadId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody List<GuardarAsistenciaRequest> request
    ) {
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return gestionService.guardarAsistencia(clubId, actividadId, fecha, request);
    }

    @GetMapping("/reportes")
    public ReportesResponse reportes(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        return gestionService.reportes(clubId);
    }
}
