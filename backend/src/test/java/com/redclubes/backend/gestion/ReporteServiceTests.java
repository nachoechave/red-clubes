package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTests {
    @Mock private ClubRepository clubRepository;
    @Mock private SocioRepository socioRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private GestionService gestionService;

    @Test
    void reporteAnualUsaPagosActivosYAsistenciasDelAnioSeleccionado() {
        Club club = new Club();
        club.setId(1L);
        club.setNombre("Club Piloto");
        Pago pago = new Pago();
        pago.setImporte(new BigDecimal("7250.50"));
        pago.setFechaPago(LocalDateTime.of(2026, 8, 5, 10, 30));
        Socio socio = new Socio(10L, "Ana", "Perez", "12345678", "ACTIVO");
        Cuota cuota = new Cuota();
        cuota.setSocio(socio);
        pago.setCuota(cuota);
        Asistencia asistencia = new Asistencia();
        asistencia.setFecha(LocalDate.of(2026, 8, 4));
        asistencia.setEstado(EstadoAsistencia.PRESENTE);

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findByClubId(1L)).thenReturn(List.of(socio));
        when(actividadRepository.countByClubIdAndEstado(1L, EstadoActividad.ACTIVA)).thenReturn(0L);
        when(pagoRepository.findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
                1L, EstadoPago.ACTIVO, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)
        )).thenReturn(List.of(pago));
        when(asistenciaRepository.findByClubIdAndFechaBetween(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of(asistencia));

        ReportesResponse response = service().reportes(1L, 2026);

        assertEquals(12, response.pagosPorMes().size());
        assertEquals(new BigDecimal("7250.50"), response.pagosPorMes().get(7).valor());
        assertEquals(new BigDecimal("1"), response.asistenciaMensual().get(7).valor());
        assertEquals(BigDecimal.ZERO, response.pagosPorMes().get(6).valor());
    }

    @Test
    void filtrosDeSocioAfectanCobranzaYDeudaSinInflarCantidadDeSocios() {
        Club club = new Club();
        club.setId(1L);
        club.setNombre("Club Piloto");
        Socio activo = new Socio(10L, "Ana", "Perez", "12345678", "ACTIVO");
        Socio inactivo = new Socio(20L, "Luis", "Diaz", "22345678", "INACTIVO");
        Cuota cuotaActiva = cuota(activo, EstadoCuota.VENCIDA, "2500.00");
        Cuota segundaCuotaActiva = cuota(activo, EstadoCuota.PENDIENTE, "1500.00");
        Cuota cuotaInactiva = cuota(inactivo, EstadoCuota.VENCIDA, "9000.00");
        Pago pagoActivo = pago(cuotaActiva, "1200.00");
        Pago pagoInactivo = pago(cuotaInactiva, "8000.00");

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findByClubId(1L)).thenReturn(List.of(activo, inactivo));
        when(actividadRepository.countByClubIdAndEstado(1L, EstadoActividad.ACTIVA)).thenReturn(0L);
        when(cuotaRepository.findByClubId(1L)).thenReturn(List.of(cuotaActiva, segundaCuotaActiva, cuotaInactiva));
        when(pagoRepository.findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
                1L, EstadoPago.ACTIVO, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)
        )).thenReturn(List.of(pagoActivo, pagoInactivo));
        when(asistenciaRepository.findByClubIdAndFechaBetween(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of());

        ReportesResponse response = service().reportes(
                1L, 2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, "ACTIVO"
        );

        assertEquals(1, response.sociosPorClub().getFirst().socios());
        assertEquals(new BigDecimal("1200.00"), response.pagosPorMes().getFirst().valor());
        assertEquals(new BigDecimal("1500.00"), response.deudaPendiente());
        assertEquals(new BigDecimal("2500.00"), response.deudaVencida());
        assertEquals(2, response.cuotasConDeuda());
        assertEquals(1, response.sociosConDeuda());
    }

    @Test
    void dashboardFiltraCobranzaYAsistenciaPorMesSolicitado() {
        Pago pago = new Pago();
        pago.setImporte(new BigDecimal("1000.25"));
        pago.setFechaPago(LocalDateTime.of(2026, 8, 5, 12, 0));
        Asistencia asistencia = new Asistencia();
        asistencia.setFecha(LocalDate.of(2026, 8, 5));
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        when(socioRepository.findByClubId(1L)).thenReturn(List.of());
        when(cuotaRepository.findByClubId(1L)).thenReturn(List.of());
        when(gestionService.listarActividades(1L)).thenReturn(List.of());
        when(pagoRepository.findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
                1L, EstadoPago.ACTIVO, LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0)
        )).thenReturn(List.of(pago));
        when(asistenciaRepository.findByClubIdAndFechaBetween(
                1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)
        )).thenReturn(List.of(asistencia));
        when(pagoRepository.findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
                1L, EstadoPago.ACTIVO, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)
        )).thenReturn(List.of(pago));
        when(asistenciaRepository.findByClubIdAndFechaBetween(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        )).thenReturn(List.of(asistencia));

        DashboardResponse response = service().dashboard(1L, YearMonth.of(2026, 8));

        assertEquals(new BigDecimal("1000.25"), response.totalCobrado());
        assertEquals(1, response.asistenciaMes());
        assertEquals(new BigDecimal("1000.25"), response.pagosPorMes().get(7).valor());
    }

    private ReporteService service() {
        return new ReporteService(
                clubRepository, socioRepository, actividadRepository, cuotaRepository,
                asistenciaRepository, pagoRepository, gestionService
        );
    }

    private Cuota cuota(Socio socio, EstadoCuota estado, String importe) {
        Cuota cuota = new Cuota();
        cuota.setSocio(socio);
        cuota.setEstado(estado);
        cuota.setImporte(new BigDecimal(importe));
        cuota.setVencimiento(LocalDate.of(2026, 1, 15));
        return cuota;
    }

    private Pago pago(Cuota cuota, String importe) {
        Pago pago = new Pago();
        pago.setCuota(cuota);
        pago.setImporte(new BigDecimal(importe));
        pago.setFechaPago(LocalDateTime.of(2026, 1, 20, 10, 0));
        return pago;
    }
}
