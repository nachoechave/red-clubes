package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioNoEncontradoException;
import com.redclubes.backend.socios.SocioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class GestionService {

    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final CuotaRepository cuotaRepository;
    private final AsistenciaRepository asistenciaRepository;

    public GestionService(
            ClubRepository clubRepository,
            SocioRepository socioRepository,
            ActividadRepository actividadRepository,
            CuotaRepository cuotaRepository,
            AsistenciaRepository asistenciaRepository
    ) {
        this.clubRepository = clubRepository;
        this.socioRepository = socioRepository;
        this.actividadRepository = actividadRepository;
        this.cuotaRepository = cuotaRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    public List<ActividadResponse> listarActividades(Long clubId) {
        return actividadRepository.findByClubId(clubId).stream()
                .map(ActividadResponse::desde)
                .toList();
    }

    public ActividadResponse crearActividad(Long clubId, CrearActividadRequest request) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));

        Actividad actividad = new Actividad();
        actividad.setClub(club);
        actividad.setNombre(request.nombre());
        actividad.setProfesor(request.profesor());
        actividad.setDias(request.dias());
        actividad.setCategoria(request.categoria());
        actividad.setIcono(request.icono() == null || request.icono().isBlank() ? "check" : request.icono());
        actividad.setCupo(request.cupo());
        actividad.setInscriptos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);

        return ActividadResponse.desde(actividadRepository.save(actividad));
    }

    public List<CuotaResponse> listarCuotas(Long clubId) {
        return cuotaRepository.findByClubId(clubId).stream()
                .sorted(Comparator.comparing(Cuota::getVencimiento).reversed())
                .map(CuotaResponse::desde)
                .toList();
    }

    public CuotaResponse registrarPago(Long cuotaId) {
        Cuota cuota = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new IllegalArgumentException("Cuota no encontrada"));
        cuota.setEstado(EstadoCuota.PAGADO);
        return CuotaResponse.desde(cuotaRepository.save(cuota));
    }

    public List<AsistenciaResponse> listarAsistencia(Long clubId, Long actividadId, LocalDate fecha) {
        return asistenciaRepository.findByClubIdAndActividadIdAndFecha(clubId, actividadId, fecha).stream()
                .map(AsistenciaResponse::desde)
                .toList();
    }

    public List<AsistenciaResponse> guardarAsistencia(Long clubId, Long actividadId, LocalDate fecha, List<GuardarAsistenciaRequest> presentes) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        return presentes.stream().map(item -> {
            Socio socio = socioRepository.findById(item.socioId())
                    .orElseThrow(() -> new SocioNoEncontradoException(item.socioId()));
            Asistencia asistencia = asistenciaRepository.findByClubIdAndActividadIdAndFecha(clubId, actividadId, fecha).stream()
                    .filter(existente -> existente.getSocio().getId().equals(item.socioId()))
                    .findFirst()
                    .orElseGet(Asistencia::new);

            asistencia.setClub(club);
            asistencia.setActividad(actividad);
            asistencia.setSocio(socio);
            asistencia.setFecha(fecha);
            asistencia.setPresente(item.presente());
            return AsistenciaResponse.desde(asistenciaRepository.save(asistencia));
        }).toList();
    }

    public DashboardResponse dashboard(Long clubId) {
        List<Socio> socios = socioRepository.findByClubId(clubId);
        List<Cuota> cuotas = cuotaRepository.findByClubId(clubId);
        List<ActividadResponse> actividades = listarActividades(clubId);
        int activos = (int) socios.stream().filter(socio -> "ACTIVO".equals(socio.getEstado())).count();
        int cuotasAlDia = (int) cuotas.stream().filter(cuota -> cuota.getEstado() == EstadoCuota.PAGADO).count();
        int morosos = (int) cuotas.stream().filter(cuota -> cuota.getEstado() == EstadoCuota.VENCIDA || cuota.getEstado() == EstadoCuota.PENDIENTE).map(cuota -> cuota.getSocio().getId()).distinct().count();
        int totalCobrado = cuotas.stream().filter(cuota -> cuota.getEstado() == EstadoCuota.PAGADO).mapToInt(Cuota::getImporte).sum();
        int asistenciaMes = (int) asistenciaRepository.findByClubId(clubId).stream().filter(Asistencia::isPresente).count();

        return new DashboardResponse(
                activos,
                socios.size(),
                cuotasAlDia,
                morosos,
                (int) actividades.stream().filter(actividad -> actividad.estado() == EstadoActividad.ACTIVA).count(),
                asistenciaMes,
                totalCobrado,
                movimientos(socios, cuotas),
                actividades.stream().limit(4).toList(),
                cuotas.stream().filter(cuota -> cuota.getEstado() != EstadoCuota.PAGADO).limit(4).map(CuotaResponse::desde).toList(),
                socios.stream().limit(4).map(socio -> new SocioResumenResponse(socio.getId(), socio.getNombre() + " " + socio.getApellido(), socio.getTelefono(), socio.getEstado())).toList(),
                pagosPorMes(cuotas),
                asistenciaPorMes(clubId)
        );
    }

    public ReportesResponse reportes(Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        List<ResumenClubResponse> clubes = List.of(new ResumenClubResponse(
                club.getNombre(),
                socioRepository.findByClubId(clubId).size(),
                (int) actividadRepository.countByClubIdAndEstado(clubId, EstadoActividad.ACTIVA)
        ));
        List<Cuota> cuotas = cuotaRepository.findByClubId(clubId);
        return new ReportesResponse(clubes, pagosPorMes(cuotas), asistenciaPorMes(clubId));
    }

    private List<MovimientoResponse> movimientos(List<Socio> socios, List<Cuota> cuotas) {
        List<MovimientoResponse> sociosMovimientos = socios.stream()
                .limit(2)
                .map(socio -> new MovimientoResponse(socio.getNombre() + " " + socio.getApellido() + " figura como " + socio.getEstado(), "Reciente", "ACTIVO".equals(socio.getEstado()) ? "good" : "bad"))
                .toList();
        List<MovimientoResponse> cuotasMovimientos = cuotas.stream()
                .limit(2)
                .map(cuota -> new MovimientoResponse(cuota.getSocio().getNombre() + " " + cuota.getSocio().getApellido() + " tiene cuota " + cuota.getEstado(), cuota.getMes(), cuota.getEstado() == EstadoCuota.PAGADO ? "good" : "bad"))
                .toList();

        return java.util.stream.Stream.concat(sociosMovimientos.stream(), cuotasMovimientos.stream()).toList();
    }

    private List<GraficoMesResponse> pagosPorMes(List<Cuota> cuotas) {
        return mesesDelReporte().stream()
                .map(month -> new GraficoMesResponse(nombreMes(month), cuotas.stream()
                        .filter(cuota -> cuota.getEstado() == EstadoCuota.PAGADO)
                        .filter(cuota -> cuota.getVencimiento().getMonth() == month)
                        .mapToInt(Cuota::getImporte)
                        .sum()))
                .toList();
    }

    private List<GraficoMesResponse> asistenciaPorMes(Long clubId) {
        List<Asistencia> asistencias = asistenciaRepository.findByClubId(clubId);
        return mesesDelReporte().stream()
                .map(month -> new GraficoMesResponse(nombreMes(month), (int) asistencias.stream()
                        .filter(Asistencia::isPresente)
                        .filter(asistencia -> asistencia.getFecha().getMonth() == month)
                        .count()))
                .toList();
    }

    private List<Month> mesesDelReporte() {
        return List.of(Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL, Month.MAY, Month.JUNE, Month.JULY);
    }

    private String nombreMes(Month month) {
        return month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-AR")).replace(".", "");
    }
}
