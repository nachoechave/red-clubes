package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(2)
public class DatosDemoInitializer implements CommandLineRunner {

    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final CuotaRepository cuotaRepository;
    private final AsistenciaRepository asistenciaRepository;

    public DatosDemoInitializer(
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

    @Override
    public void run(String... args) {
        clubRepository.findByNombre("Club San Martin").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Ana", "Ruiz", "12345678", "221 123-4567", "Calle 12 345", "Marta Ruiz", "221 555-1001", "Hermana", "ACTIVO"),
                        socio("Carlos", "Gonzalez", "14567890", "221 234-5678", "Av. 7 890", "Lucia Gonzalez", "221 555-1002", "Hija", "INACTIVO"),
                        socio("Laura", "Perez", "20123456", "221 789-0123", "Calle 8 910", "Daniel Perez", "221 555-1007", "Hermano", "ACTIVO")
                ),
                List.of(
                        actividad("Gimnasia para adultos mayores", "Marta Lopez", "Lunes y miercoles 09:00 hs", "Bienestar", "person", 30, 24),
                        actividad("Folklore", "Jose Perez", "Viernes 19:00 hs", "Cultura", "note", 25, 20)
                )
        ));

        clubRepository.findByNombre("Centro de Jubilados").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Elena", "Fernandez", "16789012", "221 345-6789", "Calle 45 678", "Pablo Fernandez", "221 555-1003", "Hijo", "ACTIVO"),
                        socio("Jose", "Martinez", "17890123", "221 456-7890", "Diagonal 80 111", "Rosa Martinez", "221 555-1004", "Esposa", "ACTIVO")
                ),
                List.of(
                        actividad("Yoga", "Laura Gomez", "Lunes y miercoles 09:00 hs", "Bienestar", "person", 25, 18),
                        actividad("Taller de tejido", "Elena Torres", "Martes 15:00 hs", "Manualidades", "palette", 15, 12)
                )
        ));

        clubRepository.findByNombre("Club Union").ifPresent(club -> cargarClub(
                club,
                List.of(
                        socio("Maria", "Lopez", "18901234", "221 567-8901", "Calle 60 222", "Sofia Lopez", "221 555-1005", "Nieta", "INACTIVO"),
                        socio("Pedro", "Sanchez", "19123456", "221 678-9012", "Av. 13 456", "Claudia Sanchez", "221 555-1006", "Hija", "ACTIVO")
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
            crearCuota(club, socio, "Junio 2026", 6000, socio.getEstado().equals("ACTIVO") ? EstadoCuota.PENDIENTE : EstadoCuota.VENCIDA, LocalDate.of(2026, 6, 10));
            crearCuota(club, socio, "Mayo 2026", 6000, EstadoCuota.PAGADO, LocalDate.of(2026, 5, 10));
        }

        cargarHistorialCuotas(club, sociosGuardados);
        cargarHistorialAsistencias(club, actividadesGuardadas, sociosGuardados);
    }

    private Socio socio(String nombre, String apellido, String dni, String telefono, String direccion, String emergenciaNombre, String emergenciaTelefono, String emergenciaRelacion, String estado) {
        Socio socio = new Socio();
        socio.setNombre(nombre);
        socio.setApellido(apellido);
        socio.setDni(dni);
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
        cuota.setMes(mes);
        cuota.setImporte(importe);
        cuota.setEstado(estado);
        cuota.setVencimiento(vencimiento);
        cuotaRepository.save(cuota);
    }

    private void cargarHistorialCuotas(Club club, List<Socio> socios) {
        int offset = offsetClub(club);
        String[] meses = {"Enero 2026", "Febrero 2026", "Marzo 2026", "Abril 2026", "Mayo 2026", "Junio 2026", "Julio 2026"};
        int[] months = {1, 2, 3, 4, 5, 6, 7};

        for (int mesIndex = 0; mesIndex < meses.length; mesIndex++) {
            for (int socioIndex = 0; socioIndex < socios.size(); socioIndex++) {
                Socio socio = socios.get(socioIndex);
                boolean pagada = ((mesIndex + socioIndex + offset) % 4) != 0 && mesIndex < 6;
                EstadoCuota estado = pagada ? EstadoCuota.PAGADO : (mesIndex < 5 ? EstadoCuota.VENCIDA : EstadoCuota.PENDIENTE);
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
