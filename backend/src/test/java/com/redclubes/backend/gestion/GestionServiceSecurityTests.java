package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.SocioRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.usuarios.UsuarioClubRepository;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.auditoria.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GestionServiceSecurityTests {

    @Mock private ClubRepository clubRepository;
    @Mock private SocioRepository socioRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private InscripcionActividadRepository inscripcionRepository;
    @Mock private UsuarioClubRepository usuarioClubRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditoriaService auditoriaService;

    @Test
    void noPermitePagarCuotaDeOtroClub() {
        when(cuotaRepository.findLockedByIdAndClubId(90L, 1L)).thenReturn(Optional.empty());
        CobranzaService service = new CobranzaService(clubRepository, socioRepository, cuotaRepository, pagoRepository, auditoriaService);

        assertThrows(IllegalArgumentException.class, () -> service.registrarPago(
                1L, 90L, new RegistrarPagoRequest(MedioPago.EFECTIVO, null), new Usuario()));
        verify(cuotaRepository).findLockedByIdAndClubId(90L, 1L);
        verify(cuotaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noPermiteInscribirSocioInactivo() {
        Club club = club(1L);
        Socio socio = socio(10L, club, "INACTIVO");
        Actividad actividad = actividad(20L, club, 5);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio));
        when(actividadRepository.findById(20L)).thenReturn(Optional.of(actividad));

        GestionService service = service();
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarActividadesDeSocio(1L, 10L, new ActualizarInscripcionesRequest(List.of(20L), null), null));
        verify(inscripcionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noPermiteSuperarCupo() {
        Club club = club(1L);
        Socio socio = socio(10L, club, "ACTIVO");
        Socio otroSocio = socio(11L, club, "ACTIVO");
        Actividad actividad = actividad(20L, club, 1);
        InscripcionActividad ocupante = inscripcion(club, otroSocio, actividad);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio));
        when(actividadRepository.findById(20L)).thenReturn(Optional.of(actividad));
        when(inscripcionRepository.findByClubIdAndSocioIdAndActividadId(1L, 10L, 20L))
                .thenReturn(Optional.empty());
        when(inscripcionRepository.findByClubIdAndActividadIdAndEstado(1L, 20L, EstadoInscripcion.ACTIVA))
                .thenReturn(List.of(ocupante));

        GestionService service = service();
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarActividadesDeSocio(1L, 10L, new ActualizarInscripcionesRequest(List.of(20L), null), null));
        verify(inscripcionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reutilizaInscripcionActivaEnLugarDeDuplicarla() {
        Club club = club(1L);
        Socio socio = socio(10L, club, "ACTIVO");
        Actividad actividad = actividad(20L, club, 2);
        InscripcionActividad existente = inscripcion(club, socio, actividad);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio));
        when(actividadRepository.findById(20L)).thenReturn(Optional.of(actividad));
        when(inscripcionRepository.findByClubIdAndSocioIdAndActividadId(1L, 10L, 20L))
                .thenReturn(Optional.of(existente));
        when(inscripcionRepository.findByClubIdAndSocioIdAndEstado(1L, 10L, EstadoInscripcion.ACTIVA))
                .thenReturn(List.of(existente));
        when(inscripcionRepository.findByClubIdAndActividadIdAndEstado(1L, 20L, EstadoInscripcion.ACTIVA))
                .thenReturn(List.of(existente));
        when(inscripcionRepository.countByClubIdAndActividadIdAndEstado(1L, 20L, EstadoInscripcion.ACTIVA)).thenReturn(1L);

        service().actualizarActividadesDeSocio(1L, 10L, new ActualizarInscripcionesRequest(List.of(20L), null), null);

        org.mockito.ArgumentCaptor<InscripcionActividad> captor = org.mockito.ArgumentCaptor.forClass(InscripcionActividad.class);
        verify(inscripcionRepository).save(captor.capture());
        assertSame(existente, captor.getValue());
    }

    @Test
    void noPermiteTomarAsistenciaDeSocioNoInscripto() {
        Club club = club(1L);
        Socio socio = socio(10L, club, "ACTIVO");
        Actividad actividad = actividad(20L, club, 10);
        actividad.setDias("Lunes");
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(actividadRepository.findById(20L)).thenReturn(Optional.of(actividad));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio));
        when(inscripcionRepository.findByClubIdAndSocioIdAndActividadId(1L, 10L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service().guardarAsistencia(
                1L,
                20L,
                LocalDate.of(2026, 8, 3),
                List.of(new GuardarAsistenciaRequest(10L, null, EstadoAsistencia.PRESENTE)),
                false,
                new Usuario()
        ));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void registraEstadoYResponsableDeAsistencia() {
        Club club = club(1L);
        Socio socio = socio(10L, club, "ACTIVO");
        Actividad actividad = actividad(20L, club, 10);
        actividad.setDias("Lunes");
        Usuario responsable = new Usuario();
        responsable.setId(30L);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(actividadRepository.findById(20L)).thenReturn(Optional.of(actividad));
        when(socioRepository.findById(10L)).thenReturn(Optional.of(socio));
        when(inscripcionRepository.findByClubIdAndSocioIdAndActividadId(1L, 10L, 20L))
                .thenReturn(Optional.of(inscripcion(club, socio, actividad)));
        when(asistenciaRepository.findByClubIdAndActividadIdAndFecha(1L, 20L, LocalDate.of(2026, 8, 3)))
                .thenReturn(List.of());
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AsistenciaResponse response = service().guardarAsistencia(
                1L,
                20L,
                LocalDate.of(2026, 8, 3),
                List.of(new GuardarAsistenciaRequest(10L, null, EstadoAsistencia.JUSTIFICADO)),
                false,
                responsable
        ).getFirst();

        assertEquals(EstadoAsistencia.JUSTIFICADO, response.estado());
        assertEquals(30L, response.usuarioResponsableId());
        assertFalse(response.presente());
    }

    private GestionService service() {
        return new GestionService(clubRepository, socioRepository, actividadRepository,
                asistenciaRepository, inscripcionRepository, usuarioClubRepository, auditoriaService);
    }

    private Club club(Long id) {
        Club club = new Club();
        club.setId(id);
        return club;
    }

    private Socio socio(Long id, Club club, String estado) {
        Socio socio = new Socio();
        socio.setId(id);
        socio.setClub(club);
        socio.setEstado(estado);
        return socio;
    }

    private Actividad actividad(Long id, Club club, int cupo) {
        Actividad actividad = new Actividad();
        actividad.setId(id);
        actividad.setClub(club);
        actividad.setCupo(cupo);
        actividad.setEstado(EstadoActividad.ACTIVA);
        return actividad;
    }

    private InscripcionActividad inscripcion(Club club, Socio socio, Actividad actividad) {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setClub(club);
        inscripcion.setSocio(socio);
        inscripcion.setActividad(actividad);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        return inscripcion;
    }
}
