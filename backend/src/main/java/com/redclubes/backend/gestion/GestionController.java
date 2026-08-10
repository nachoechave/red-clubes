package com.redclubes.backend.gestion;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/clubes/{clubId}")
public class GestionController {

    private final GestionService gestionService;
    private final ReporteService reporteService;
    private final AuthService authService;

    public GestionController(GestionService gestionService, ReporteService reporteService, AuthService authService) {
        this.gestionService = gestionService;
        this.reporteService = reporteService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @RequestParam(required = false) String periodo
    ) {
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        YearMonth periodoSeleccionado;
        try {
            periodoSeleccionado = periodo == null ? YearMonth.now() : YearMonth.parse(periodo);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("El periodo debe tener formato YYYY-MM");
        }
        return reporteService.dashboard(clubId, periodoSeleccionado);
    }

    @GetMapping("/actividades")
    public List<ActividadResponse> actividades(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId
    ) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        if (authService.puedeOperarClub(usuario, clubId)) {
            return gestionService.listarActividades(clubId);
        }

        return gestionService.listarActividades(clubId, authService.actividadesPermitidas(usuario, clubId));
    }

    @PostMapping("/actividades")
    public ActividadResponse crearActividad(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @Valid @RequestBody CrearActividadRequest request
    ) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return gestionService.crearActividad(clubId, request, actor);
    }

    @PutMapping("/actividades/{actividadId}")
    public ActividadResponse actualizarActividad(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long actividadId,
            @Valid @RequestBody ActualizarActividadRequest request
    ) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirAdministradorDeClub(authorizationHeader, clubId);
        return gestionService.actualizarActividad(clubId, actividadId, request, actor);
    }

    @GetMapping("/socios/{socioId}/actividades")
    public List<ActividadResponse> actividadesDeSocio(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long socioId
    ) {
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return gestionService.listarActividadesDeSocio(clubId, socioId);
    }

    @PutMapping("/socios/{socioId}/actividades")
    public List<ActividadResponse> actualizarActividadesDeSocio(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long socioId,
            @RequestBody ActualizarInscripcionesRequest request
    ) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        return gestionService.actualizarActividadesDeSocio(clubId, socioId, request, usuario);
    }

    @GetMapping("/actividades/{actividadId}/asistencias/{fecha}")
    public List<AsistenciaResponse> asistencias(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long actividadId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        authService.exigirAccesoAActividad(usuario, clubId, actividadId);
        return gestionService.listarAsistencia(clubId, actividadId, fecha);
    }

    @PostMapping("/actividades/{actividadId}/asistencias/{fecha}")
    public List<AsistenciaResponse> guardarAsistencia(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @PathVariable Long actividadId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Valid @RequestBody List<@Valid GuardarAsistenciaRequest> request
    ) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        authService.exigirAccesoAClub(authorizationHeader, clubId);
        authService.exigirAccesoAActividad(usuario, clubId, actividadId);
        boolean restringirAFechaActual = !authService.puedeOperarClub(usuario, clubId);
        return gestionService.guardarAsistencia(clubId, actividadId, fecha, request, restringirAFechaActual, usuario);
    }

    @GetMapping("/reportes")
    public ReportesResponse reportes(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long actividadId,
            @RequestParam(required = false) String estadoSocio
    ) {
        authService.exigirOperadorDeClub(authorizationHeader, clubId);
        int anioSeleccionado = anio == null ? YearMonth.now().getYear() : anio;
        if (anioSeleccionado < 2000 || anioSeleccionado > 2100) {
            throw new IllegalArgumentException("El anio debe estar entre 2000 y 2100");
        }
        if ((desde == null) != (hasta == null)) {
            throw new IllegalArgumentException("Indica desde y hasta para filtrar por rango");
        }
        LocalDate fechaDesde = desde == null ? LocalDate.of(anioSeleccionado, 1, 1) : desde;
        LocalDate fechaHasta = hasta == null ? LocalDate.of(anioSeleccionado, 12, 31) : hasta;
        if (fechaDesde.isAfter(fechaHasta) || fechaDesde.getYear() != anioSeleccionado || fechaHasta.getYear() != anioSeleccionado) {
            throw new IllegalArgumentException("El rango debe ser valido y pertenecer al anio seleccionado");
        }
        if (actividadId != null) {
            gestionService.listarActividades(clubId).stream()
                    .filter(actividad -> actividad.id().equals(actividadId))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        }
        if (estadoSocio != null && !List.of("ACTIVO", "INACTIVO").contains(estadoSocio)) {
            throw new IllegalArgumentException("Estado de socio invalido");
        }
        return reporteService.reportes(clubId, anioSeleccionado, fechaDesde, fechaHasta, actividadId, estadoSocio);
    }
}
