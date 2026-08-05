package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReporteService {
    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final CuotaRepository cuotaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final PagoRepository pagoRepository;
    private final GestionService gestionService;

    public ReporteService(
            ClubRepository clubRepository,
            SocioRepository socioRepository,
            ActividadRepository actividadRepository,
            CuotaRepository cuotaRepository,
            AsistenciaRepository asistenciaRepository,
            PagoRepository pagoRepository,
            GestionService gestionService
    ) {
        this.clubRepository = clubRepository;
        this.socioRepository = socioRepository;
        this.actividadRepository = actividadRepository;
        this.cuotaRepository = cuotaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.pagoRepository = pagoRepository;
        this.gestionService = gestionService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long clubId, YearMonth periodo) {
        List<Socio> socios = socioRepository.findByClubId(clubId);
        Set<Long> socioIds = socios.stream().map(Socio::getId).collect(Collectors.toSet());
        List<Cuota> cuotas = cuotaRepository.findByClubId(clubId).stream()
                .filter(cuota -> cuota.getSocio() != null && socioIds.contains(cuota.getSocio().getId()))
                .toList();
        List<ActividadResponse> actividades = gestionService.listarActividades(clubId);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        List<Pago> pagosDelMes = pagos(clubId, desde, hasta);
        List<Asistencia> asistenciasDelMes = asistenciaRepository.findByClubIdAndFechaBetween(clubId, desde, hasta);
        int activos = (int) socios.stream().filter(socio -> "ACTIVO".equals(socio.getEstado())).count();
        int morosos = (int) socios.stream().filter(socio -> !socioEstaAlDia(socio, cuotas)).count();

        return new DashboardResponse(
                activos,
                socios.size(),
                socios.size() - morosos,
                morosos,
                (int) actividades.stream().filter(actividad -> actividad.estado() == EstadoActividad.ACTIVA).count(),
                (int) asistenciasDelMes.stream().filter(Asistencia::isPresente).count(),
                importeTotal(pagosDelMes),
                movimientos(socios, cuotas),
                actividades.stream().limit(4).toList(),
                cuotas.stream().filter(cuota -> cuota.getEstado() != EstadoCuota.PAGADA && cuota.getEstado() != EstadoCuota.ANULADA)
                        .limit(4).map(CuotaResponse::desde).toList(),
                socios.stream().limit(4).map(socio -> new SocioResumenResponse(
                        socio.getId(), socio.getNombre() + " " + socio.getApellido(), socio.getTelefono(), socio.getEstado()
                )).toList(),
                pagosPorMes(clubId, periodo.getYear()),
                asistenciaPorMes(clubId, periodo.getYear())
        );
    }

    @Transactional(readOnly = true)
    public ReportesResponse reportes(Long clubId, int anio) {
        return reportes(clubId, anio, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), null, null);
    }

    @Transactional(readOnly = true)
    public ReportesResponse reportes(
            Long clubId, int anio, LocalDate desde, LocalDate hasta, Long actividadId, String estadoSocio
    ) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        List<Socio> socios = socioRepository.findByClubId(clubId).stream()
                .filter(socio -> estadoSocio == null || estadoSocio.equals(socio.getEstado()))
                .toList();
        List<ResumenClubResponse> clubes = List.of(new ResumenClubResponse(
                club.getNombre(),
                socios.size(),
                actividadId == null
                        ? (int) actividadRepository.countByClubIdAndEstado(clubId, EstadoActividad.ACTIVA)
                        : 1
        ));
        Set<Long> socioIds = socios.stream().map(Socio::getId).collect(Collectors.toSet());
        List<Cuota> cuotasDeuda = cuotaRepository.findByClubId(clubId).stream()
                .filter(cuota -> cuota.getSocio() != null && socioIds.contains(cuota.getSocio().getId()))
                .filter(cuota -> !cuota.getVencimiento().isBefore(desde) && !cuota.getVencimiento().isAfter(hasta))
                .filter(cuota -> cuota.getEstado() == EstadoCuota.PENDIENTE || cuota.getEstado() == EstadoCuota.VENCIDA)
                .toList();
        return new ReportesResponse(
                clubes,
                pagosPorMes(clubId, anio, desde, hasta, socioIds),
                asistenciaPorMes(clubId, anio, desde, hasta, actividadId),
                sumarCuotas(cuotasDeuda, EstadoCuota.PENDIENTE),
                sumarCuotas(cuotasDeuda, EstadoCuota.VENCIDA),
                cuotasDeuda.size(),
                cuotasDeuda.stream().map(cuota -> cuota.getSocio().getId()).distinct().count()
        );
    }

    private BigDecimal sumarCuotas(List<Cuota> cuotas, EstadoCuota estado) {
        return cuotas.stream().filter(cuota -> cuota.getEstado() == estado)
                .map(Cuota::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean socioEstaAlDia(Socio socio, List<Cuota> cuotas) {
        return cuotas.stream()
                .filter(cuota -> cuota.getSocio().getId().equals(socio.getId()))
                .noneMatch(cuota -> cuota.getEstado() == EstadoCuota.VENCIDA || cuota.getEstado() == EstadoCuota.PENDIENTE);
    }

    private List<MovimientoResponse> movimientos(List<Socio> socios, List<Cuota> cuotas) {
        List<MovimientoResponse> sociosMovimientos = socios.stream()
                .limit(2)
                .map(socio -> new MovimientoResponse(
                        socio.getNombre() + " " + socio.getApellido() + " figura como " + socio.getEstado(),
                        "Reciente", "ACTIVO".equals(socio.getEstado()) ? "good" : "bad"
                )).toList();
        List<MovimientoResponse> cuotasMovimientos = cuotas.stream()
                .limit(2)
                .map(cuota -> new MovimientoResponse(
                        cuota.getSocio().getNombre() + " " + cuota.getSocio().getApellido() + " tiene cuota " + cuota.getEstado(),
                        cuota.getPeriodo(), cuota.getEstado() == EstadoCuota.PAGADA ? "good" : "bad"
                )).toList();
        return java.util.stream.Stream.concat(sociosMovimientos.stream(), cuotasMovimientos.stream()).toList();
    }

    private List<GraficoMesResponse> pagosPorMes(Long clubId, int anio) {
        return pagosPorMes(clubId, anio, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31));
    }

    private List<GraficoMesResponse> pagosPorMes(Long clubId, int anio, LocalDate desde, LocalDate hasta) {
        return pagosPorMes(clubId, anio, desde, hasta, null);
    }

    private List<GraficoMesResponse> pagosPorMes(
            Long clubId, int anio, LocalDate desde, LocalDate hasta, Set<Long> socioIds
    ) {
        List<Pago> pagos = pagos(clubId, desde, hasta).stream()
                .filter(pago -> socioIds == null
                        || pago.getCuota() != null && pago.getCuota().getSocio() != null
                        && socioIds.contains(pago.getCuota().getSocio().getId()))
                .toList();
        return mesesDelReporte().stream()
                .map(mes -> new GraficoMesResponse(nombreMes(mes), importeTotal(pagos.stream()
                        .filter(pago -> pago.getFechaPago().getMonth() == mes)
                        .toList())))
                .toList();
    }

    private List<GraficoMesResponse> asistenciaPorMes(Long clubId, int anio) {
        return asistenciaPorMes(clubId, anio, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), null);
    }

    private List<GraficoMesResponse> asistenciaPorMes(
            Long clubId, int anio, LocalDate desde, LocalDate hasta, Long actividadId
    ) {
        List<Asistencia> asistencias = asistenciaRepository.findByClubIdAndFechaBetween(clubId, desde, hasta).stream()
                .filter(asistencia -> actividadId == null || asistencia.getActividad().getId().equals(actividadId))
                .toList();
        return mesesDelReporte().stream()
                .map(mes -> new GraficoMesResponse(nombreMes(mes), BigDecimal.valueOf(asistencias.stream()
                        .filter(Asistencia::isPresente)
                        .filter(asistencia -> asistencia.getFecha().getMonth() == mes)
                        .count())))
                .toList();
    }

    private List<Pago> pagos(Long clubId, LocalDate desde, LocalDate hasta) {
        return pagoRepository.findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
                clubId, EstadoPago.ACTIVO, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()
        );
    }

    private BigDecimal importeTotal(List<Pago> pagos) {
        return pagos.stream().map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Month> mesesDelReporte() {
        return List.of(Month.values());
    }

    private String nombreMes(Month mes) {
        return mes.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-AR")).replace(".", "");
    }
}
