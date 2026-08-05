package com.redclubes.backend.config;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.clubes.EstadoClub;
import com.redclubes.backend.gestion.Actividad;
import com.redclubes.backend.gestion.ActividadRepository;
import com.redclubes.backend.gestion.Asistencia;
import com.redclubes.backend.gestion.AsistenciaRepository;
import com.redclubes.backend.gestion.Cuota;
import com.redclubes.backend.gestion.CuotaRepository;
import com.redclubes.backend.gestion.EstadoActividad;
import com.redclubes.backend.gestion.EstadoCuota;
import com.redclubes.backend.gestion.EstadoInscripcion;
import com.redclubes.backend.gestion.InscripcionActividad;
import com.redclubes.backend.gestion.InscripcionActividadRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseIntegrityTests {

    @Autowired private ClubRepository clubRepository;
    @Autowired private SocioRepository socioRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private InscripcionActividadRepository inscripcionRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private AsistenciaRepository asistenciaRepository;

    @Test
    void dniPuedeRepetirseEnClubesDistintosPeroNoEnElMismoClub() {
        Club clubA = guardarClub("Club A");
        Club clubB = guardarClub("Club B");
        socioRepository.saveAndFlush(socio(clubA, "12345678", "Ana"));

        assertDoesNotThrow(() -> socioRepository.saveAndFlush(socio(clubB, "12345678", "Berta")));
        assertThrows(DataIntegrityViolationException.class,
                () -> socioRepository.saveAndFlush(socio(clubA, "12345678", "Carla")));
    }

    @Test
    void noPermiteDosInscripcionesParaLaMismaActividadYSocio() {
        Club club = guardarClub("Club inscripciones");
        Socio socio = socioRepository.saveAndFlush(socio(club, "22345678", "Dora"));
        Actividad actividad = actividadRepository.saveAndFlush(actividad(club));
        inscripcionRepository.saveAndFlush(inscripcion(club, socio, actividad));

        assertThrows(DataIntegrityViolationException.class,
                () -> inscripcionRepository.saveAndFlush(inscripcion(club, socio, actividad)));
    }

    @Test
    void noPermiteDosCuotasDelMismoPeriodo() {
        Club club = guardarClub("Club cuotas");
        Socio socio = socioRepository.saveAndFlush(socio(club, "32345678", "Elena"));
        cuotaRepository.saveAndFlush(cuota(club, socio));

        assertThrows(DataIntegrityViolationException.class,
                () -> cuotaRepository.saveAndFlush(cuota(club, socio)));
    }

    @Test
    void noPermiteDuplicarAsistenciaPorFecha() {
        Club club = guardarClub("Club asistencias");
        Socio socio = socioRepository.saveAndFlush(socio(club, "42345678", "Fabiana"));
        Actividad actividad = actividadRepository.saveAndFlush(actividad(club));
        asistenciaRepository.saveAndFlush(asistencia(club, socio, actividad));

        assertThrows(DataIntegrityViolationException.class,
                () -> asistenciaRepository.saveAndFlush(asistencia(club, socio, actividad)));
    }

    private Club guardarClub(String nombre) {
        return clubRepository.saveAndFlush(new Club(nombre, "Direccion de prueba", EstadoClub.ACTIVO));
    }

    private Socio socio(Club club, String dni, String nombre) {
        Socio socio = new Socio(null, nombre, "Apellido", dni, "ACTIVO");
        socio.setClub(club);
        socio.setFechaAlta(LocalDate.now());
        return socio;
    }

    private Actividad actividad(Club club) {
        Actividad actividad = new Actividad();
        actividad.setClub(club);
        actividad.setNombre("Yoga");
        actividad.setProfesor("Profesor de prueba");
        actividad.setDias("Lunes");
        actividad.setCategoria("Salud");
        actividad.setIcono("check");
        actividad.setCupo(10);
        actividad.setInscriptos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);
        return actividad;
    }

    private InscripcionActividad inscripcion(Club club, Socio socio, Actividad actividad) {
        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setClub(club);
        inscripcion.setSocio(socio);
        inscripcion.setActividad(actividad);
        inscripcion.setFechaInscripcion(LocalDate.now());
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        return inscripcion;
    }

    private Cuota cuota(Club club, Socio socio) {
        Cuota cuota = new Cuota();
        cuota.setClub(club);
        cuota.setSocio(socio);
        cuota.setPeriodo("2026-08");
        cuota.setImporte(1000);
        cuota.setEstado(EstadoCuota.PENDIENTE);
        cuota.setVencimiento(LocalDate.of(2026, 8, 10));
        return cuota;
    }

    private Asistencia asistencia(Club club, Socio socio, Actividad actividad) {
        Asistencia asistencia = new Asistencia();
        asistencia.setClub(club);
        asistencia.setSocio(socio);
        asistencia.setActividad(actividad);
        asistencia.setFecha(LocalDate.of(2026, 8, 5));
        asistencia.setPresente(true);
        return asistencia;
    }
}
