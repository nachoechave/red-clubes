package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "redclubes.demo.enabled", havingValue = "true")
@Order(2)
public class DatosDemoInitializer implements CommandLineRunner {

    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final CuotaRepository cuotaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final PagoRepository pagoRepository;

    public DatosDemoInitializer(
            ClubRepository clubRepository,
            SocioRepository socioRepository,
            ActividadRepository actividadRepository,
            CuotaRepository cuotaRepository,
            AsistenciaRepository asistenciaRepository,
            InscripcionActividadRepository inscripcionActividadRepository,
            PagoRepository pagoRepository
    ) {
        this.clubRepository = clubRepository;
        this.socioRepository = socioRepository;
        this.actividadRepository = actividadRepository;
        this.cuotaRepository = cuotaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.inscripcionActividadRepository = inscripcionActividadRepository;
        this.pagoRepository = pagoRepository;
    }

    @Override
    public void run(String... args) {
        clubRepository.findByNombre("Club San Martin").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Ana", "Ruiz", "12345678", "ana.ruiz@email.com", LocalDate.of(1952, 6, 12), "221 123-4567", "Calle 12 345", "Marta Ruiz", "221 555-1001", "Hermana", "ACTIVO"),
                        socio("Carlos", "Gonzalez", "14567890", "carlos.gonzalez@email.com", LocalDate.of(1948, 9, 4), "221 234-5678", "Av. 7 890", "Lucia Gonzalez", "221 555-1002", "Hija", "INACTIVO"),
                        socio("Laura", "Perez", "20123456", "laura.perez@email.com", LocalDate.of(1956, 6, 24), "221 789-0123", "Calle 8 910", "Daniel Perez", "221 555-1007", "Hermano", "ACTIVO")
                ),
                List.of(
                        actividad("Gimnasia para adultos mayores", "Marta Lopez", "Lunes y miercoles 09:00 hs", "Bienestar", "person", 30, 24),
                        actividad("Folklore", "Jose Perez", "Viernes 19:00 hs", "Cultura", "note", 25, 20)
                )
        ));

        clubRepository.findByNombre("Centro de Jubilados").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Elena", "Fernandez", "16789012", "elena.fernandez@email.com", LocalDate.of(1951, 3, 17), "221 345-6789", "Calle 45 678", "Pablo Fernandez", "221 555-1003", "Hijo", "ACTIVO"),
                        socio("Jose", "Martinez", "17890123", "jose.martinez@email.com", LocalDate.of(1949, 6, 5), "221 456-7890", "Diagonal 80 111", "Rosa Martinez", "221 555-1004", "Esposa", "ACTIVO")
                ),
                List.of(
                        actividad("Yoga", "Laura Gomez", "Lunes y miercoles 09:00 hs", "Bienestar", "person", 25, 18),
                        actividad("Taller de tejido", "Elena Torres", "Martes 15:00 hs", "Manualidades", "palette", 15, 12)
                )
        ));

        clubRepository.findByNombre("Club Union").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Maria", "Lopez", "18901234", "maria.lopez@email.com", LocalDate.of(1954, 12, 21), "221 567-8901", "Calle 60 222", "Sofia Lopez", "221 555-1005", "Nieta", "INACTIVO"),
                        socio("Pedro", "Sanchez", "19123456", "pedro.sanchez@email.com", LocalDate.of(1950, 6, 28), "221 678-9012", "Av. 13 456", "Claudia Sanchez", "221 555-1006", "Hija", "ACTIVO")
                ),
                List.of(
                        actividad("Teatro", "Juan Perez", "Viernes 17:00 hs", "Cultura", "mask", 20, 12),
                        actividad("Caminata saludable", "Carlos Gomez", "Sabados 08:30 hs", "Actividad fisica", "check", 40, 30)
                )
        ));
    }

    private void cargarClub(Club club, List<Socio> socios, List<Actividad> actividades) {
        List<Socio> sociosGuardados = socios.stream().map(socio -> {
            if (socioRepository.existsByDni(socio.getDni())) {
                return socioRepository.findByDni(socio.getDni()).orElse(socio);
            }
            socio.setClub(club);
            return socioRepository.save(socio);
        }).toList();

        List<Actividad> actividadesGuardadas = actividades.stream().map(actividad -> {
            Actividad existente = actividadRepository.findByClubIdAndNombre(club.getId(), actividad.getNombre()).orElse(null);
            if (existente != null) {
                return existente;
            }
            actividad.setClub(club);
            return actividadRepository.save(actividad);
        }).toList();

        for (Socio socio : sociosGuardados) {
            crearCuota(club, socio, "2026-06", 6000, socio.getEstado().equals("ACTIVO") ? EstadoCuota.PENDIENTE : EstadoCuota.VENCIDA, LocalDate.of(2026, 6, 10));
            crearCuota(club, socio, "2026-05", 6000, EstadoCuota.PAGADA, LocalDate.of(2026, 5, 10));
        }

        cargarHistorialCuotas(club, sociosGuardados);
        cargarInscripciones(club, sociosGuardados, actividadesGuardadas);
        cargarHistorialAsistencias(club, actividadesGuardadas, sociosGuardados);
    }

    private Socio socio(String nombre, String apellido, String dni, String email, LocalDate fechaNacimiento, String telefono, String direccion, String emergenciaNombre, String emergenciaTelefono, String emergenciaRelacion, String estado) {
        Socio socio = new Socio();
        socio.setNombre(nombre);
        socio.setApellido(apellido);
        socio.setDni(dni);
        socio.setEmail(email);
        socio.setFechaNacimiento(fechaNacimiento);
        socio.setFechaAlta(LocalDate.of(2022, 4, 1));
        socio.setTelefono(telefono);
        socio.setDireccion(direccion);
        socio.setEmergenciaNombre(emergenciaNombre);
        socio.setEmergenciaTelefono(emergenciaTelefono);
        socio.setEmergenciaRelacion(emergenciaRelacion);
        socio.setEstado(estado);
        return socio;
    }

    private Actividad actividad(String nombre, String profesor, String dias, String categoria, String icono, int cupo, int inscriptos) {
        Actividad actividad = new Actividad();
        actividad.setNombre(nombre);
        actividad.setProfesor(profesor);
        actividad.setDias(dias);
        actividad.setCategoria(categoria);
        actividad.setIcono(icono);
        actividad.setCupo(cupo);
        actividad.setInscriptos(inscriptos);
        actividad.setEstado(EstadoActividad.ACTIVA);
        return actividad;
    }

    private void crearCuota(Club club, Socio socio, String mes, int importe, EstadoCuota estado, LocalDate vencimiento) {
        if (cuotaRepository.existsByClubIdAndSocioIdAndMes(club.getId(), socio.getId(), mes)) {
            return;
        }

        Cuota cuota = new Cuota();
        cuota.setClub(club);
        cuota.setSocio(socio);
        cuota.setPeriodo(mes);
        cuota.setImporte(importe);
        cuota.setFechaEmision(vencimiento.withDayOfMonth(1));
        cuota.setEstado(estado);
        cuota.setVencimiento(vencimiento);
        Cuota guardada = cuotaRepository.save(cuota);
        if (estado == EstadoCuota.PAGADA
                && !pagoRepository.existsByClubIdAndCuotaIdAndEstado(club.getId(), guardada.getId(), EstadoPago.ACTIVO)) {
            Pago pago = new Pago();
            pago.setClub(club);
            pago.setCuota(guardada);
            pago.setImporte(guardada.getImporte());
            pago.setFechaPago(vencimiento.atStartOfDay());
            pago.setMedioPago(MedioPago.MIGRACION);
            pago.setObservaciones("Pago generado como dato demo");
            pago.setEstado(EstadoPago.ACTIVO);
            pagoRepository.save(pago);
        }
    }

    private void cargarHistorialCuotas(Club club, List<Socio> socios) {
        int offset = offsetClub(club);
        String[] meses = {"2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};
        int[] months = {1, 2, 3, 4, 5, 6, 7};

        for (int mesIndex = 0; mesIndex < meses.length; mesIndex++) {
            for (int socioIndex = 0; socioIndex < socios.size(); socioIndex++) {
                Socio socio = socios.get(socioIndex);
                boolean pagada = ((mesIndex + socioIndex + offset) % 4) != 0 && mesIndex < 6;
                EstadoCuota estado = pagada ? EstadoCuota.PAGADA : (mesIndex < 5 ? EstadoCuota.VENCIDA : EstadoCuota.PENDIENTE);
                int importe = 4500 + (offset * 500);
                crearCuota(club, socio, meses[mesIndex], importe, estado, LocalDate.of(2026, months[mesIndex], 10));
            }
        }
    }

    private void cargarHistorialAsistencias(Club club, List<Actividad> actividades, List<Socio> socios) {
        int offset = offsetClub(club);

        for (int mes = 1; mes <= 7; mes++) {
            LocalDate fecha = LocalDate.of(2026, mes, 15);
            for (int actividadIndex = 0; actividadIndex < actividades.size(); actividadIndex++) {
                Actividad actividad = actividades.get(actividadIndex);
                for (int socioIndex = 0; socioIndex < socios.size(); socioIndex++) {
                    Socio socio = socios.get(socioIndex);
                    boolean presente = ((mes + actividadIndex + socioIndex + offset) % (2 + offset)) != 0;
                    crearAsistencia(club, actividad, socio, fecha, presente);
                }
            }
        }
    }

    private void cargarInscripciones(Club club, List<Socio> socios, List<Actividad> actividades) {
        for (int socioIndex = 0; socioIndex < socios.size(); socioIndex++) {
            Socio socio = socios.get(socioIndex);
            for (int actividadIndex = 0; actividadIndex < actividades.size(); actividadIndex++) {
                if ((socioIndex + actividadIndex) % 2 == 0) {
                    crearInscripcion(club, socio, actividades.get(actividadIndex));
                }
            }
        }
        actividades.forEach(actividad -> {
            int total = inscripcionActividadRepository.findByClubIdAndActividadIdAndEstado(club.getId(), actividad.getId(), EstadoInscripcion.ACTIVA).size();
            actividad.setInscriptos(total);
            actividadRepository.save(actividad);
        });
    }

    private void crearInscripcion(Club club, Socio socio, Actividad actividad) {
        InscripcionActividad inscripcion = inscripcionActividadRepository.findByClubIdAndSocioIdAndActividadId(club.getId(), socio.getId(), actividad.getId())
                .orElseGet(InscripcionActividad::new);
        inscripcion.setClub(club);
        inscripcion.setSocio(socio);
        inscripcion.setActividad(actividad);
        inscripcion.setFechaInscripcion(inscripcion.getFechaInscripcion() == null ? LocalDate.of(2026, 4, 1) : inscripcion.getFechaInscripcion());
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcionActividadRepository.save(inscripcion);
    }

    private void crearAsistencia(Club club, Actividad actividad, Socio socio, LocalDate fecha, boolean presente) {
        if (asistenciaRepository.existsByClubIdAndActividadIdAndSocioIdAndFecha(club.getId(), actividad.getId(), socio.getId(), fecha)) {
            return;
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setClub(club);
        asistencia.setActividad(actividad);
        asistencia.setSocio(socio);
        asistencia.setFecha(fecha);
        asistencia.setPresente(presente);
        asistenciaRepository.save(asistencia);
    }

    private int offsetClub(Club club) {
        if (club.getNombre().contains("San Martin")) {
            return 1;
        }
        if (club.getNombre().contains("Jubilados")) {
            return 2;
        }
        return 3;
    }
}
