package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.auditoria.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobranzaServiceTests {
    @Mock private ClubRepository clubRepository;
    @Mock private SocioRepository socioRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditoriaService auditoriaService;

    @Test
    void generacionMensualEsIdempotente() {
        Club club = club(1L);
        Socio socio = socio(10L, club);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findByClubId(1L)).thenReturn(List.of(socio));
        when(cuotaRepository.existsByClubIdAndSocioIdAndPeriodo(1L, 10L, "2026-08"))
                .thenReturn(false, true);
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GenerarCuotasRequest request = new GenerarCuotasRequest(
                "2026-08", new BigDecimal("6500.00"), LocalDate.of(2026, 8, 10)
        );

        Usuario actor = new Usuario();
        GeneracionCuotasResponse primera = service().generarCuotas(1L, request, actor);
        GeneracionCuotasResponse segunda = service().generarCuotas(1L, request, actor);

        assertEquals(1, primera.creadas());
        assertEquals(0, segunda.creadas());
        assertEquals(1, segunda.omitidas());
    }

    @Test
    void noRegistraDosPagosActivosParaLaMismaCuota() {
        Cuota cuota = cuota(50L, club(1L), socio(10L, club(1L)));
        when(cuotaRepository.findLockedByIdAndClubId(50L, 1L)).thenReturn(Optional.of(cuota));
        when(pagoRepository.existsByClubIdAndCuotaIdAndEstado(1L, 50L, EstadoPago.ACTIVO)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service().registrarPago(
                1L, 50L, new RegistrarPagoRequest(MedioPago.EFECTIVO, null), new Usuario()
        ));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void pagoCompletoMarcaLaCuotaComoPagada() {
        Club club = club(1L);
        Cuota cuota = cuota(50L, club, socio(10L, club));
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        when(cuotaRepository.findLockedByIdAndClubId(50L, 1L)).thenReturn(Optional.of(cuota));
        when(pagoRepository.existsByClubIdAndCuotaIdAndEstado(1L, 50L, EstadoPago.ACTIVO)).thenReturn(false);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoResponse pago = service().registrarPago(
                1L, 50L, new RegistrarPagoRequest(MedioPago.TRANSFERENCIA, "Comprobante validado"), usuario
        );

        assertEquals(EstadoCuota.PAGADA, cuota.getEstado());
        assertEquals(new BigDecimal("6500.00"), pago.importe());
        assertEquals(30L, pago.usuarioResponsableId());
        verify(cuotaRepository).save(cuota);
    }

    @Test
    void anulacionConservaHistorialYReabreLaCuota() {
        Club club = club(1L);
        Cuota cuota = cuota(50L, club, socio(10L, club));
        cuota.setVencimiento(LocalDate.now().plusDays(5));
        cuota.setEstado(EstadoCuota.PAGADA);
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        Pago pago = new Pago();
        pago.setClub(club);
        pago.setCuota(cuota);
        pago.setImporte(cuota.getImporte());
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setEstado(EstadoPago.ACTIVO);
        when(cuotaRepository.findLockedByIdAndClubId(50L, 1L)).thenReturn(Optional.of(cuota));
        when(pagoRepository.findByIdAndClubIdAndCuotaId(70L, 1L, 50L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        PagoResponse response = service().anularPago(
                1L, 50L, 70L, new AnularPagoRequest("Carga duplicada"), usuario
        );

        assertEquals(EstadoPago.ANULADO, response.estado());
        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
        assertEquals(30L, response.usuarioAnulacionId());
        verify(pagoRepository).save(pago);
    }

    private CobranzaService service() {
        return new CobranzaService(clubRepository, socioRepository, cuotaRepository, pagoRepository, auditoriaService);
    }

    private Club club(Long id) {
        Club club = new Club();
        club.setId(id);
        return club;
    }

    private Socio socio(Long id, Club club) {
        Socio socio = new Socio();
        socio.setId(id);
        socio.setClub(club);
        socio.setNombre("Ada");
        socio.setApellido("Lovelace");
        socio.setDni("12345678");
        socio.setEstado("ACTIVO");
        return socio;
    }

    private Cuota cuota(Long id, Club club, Socio socio) {
        Cuota cuota = new Cuota();
        cuota.setId(id);
        cuota.setClub(club);
        cuota.setSocio(socio);
        cuota.setPeriodo("2026-08");
        cuota.setImporte(new BigDecimal("6500.00"));
        cuota.setFechaEmision(LocalDate.of(2026, 8, 1));
        cuota.setVencimiento(LocalDate.of(2026, 8, 10));
        cuota.setEstado(EstadoCuota.PENDIENTE);
        return cuota;
    }
}
