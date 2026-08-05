package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioNoEncontradoException;
import com.redclubes.backend.socios.SocioRepository;
import com.redclubes.backend.usuarios.EstadoUsuario;
import com.redclubes.backend.usuarios.RolClub;
import com.redclubes.backend.usuarios.RolUsuario;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.usuarios.UsuarioClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GestionService {

    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final ActividadRepository actividadRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final AuditoriaService auditoriaService;

    public GestionService(
            ClubRepository clubRepository,
            SocioRepository socioRepository,
            ActividadRepository actividadRepository,
            AsistenciaRepository asistenciaRepository,
            InscripcionActividadRepository inscripcionActividadRepository,
            UsuarioClubRepository usuarioClubRepository,
            AuditoriaService auditoriaService
    ) {
        this.clubRepository = clubRepository;
        this.socioRepository = socioRepository;
        this.actividadRepository = actividadRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.inscripcionActividadRepository = inscripcionActividadRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<ActividadResponse> listarActividades(Long clubId) {
        return actividadRepository.findByClubId(clubId).stream()
                .map(this::actividadResponse)
                .toList();
    }

    public List<ActividadResponse> listarActividades(Long clubId, List<Long> actividadIdsPermitidas) {
        if (actividadIdsPermitidas == null) {
            return listarActividades(clubId);
        }

        Set<Long> permitidas = new HashSet<>(actividadIdsPermitidas);
        return actividadRepository.findByClubId(clubId).stream()
                .filter(actividad -> permitidas.contains(actividad.getId()))
                .map(this::actividadResponse)
                .toList();
    }

    public List<ActividadResponse> listarActividadesDeSocio(Long clubId, Long socioId) {
        return inscripcionActividadRepository.findByClubIdAndSocioIdAndEstado(clubId, socioId, EstadoInscripcion.ACTIVA).stream()
                .filter(inscripcion -> inscripcionValidaParaClub(inscripcion, clubId))
                .map(InscripcionActividad::getActividad)
                .map(this::actividadResponse)
                .toList();
    }

    @Transactional
    public List<ActividadResponse> actualizarActividadesDeSocio(
            Long clubId,
            Long socioId,
            ActualizarInscripcionesRequest request,
            Usuario usuarioResponsable
    ) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Socio socio = socioRepository.findById(socioId).orElseThrow(() -> new SocioNoEncontradoException(socioId));
        if (socio.getClub() == null || !socio.getClub().getId().equals(clubId)) {
            throw new IllegalArgumentException("El socio no pertenece al club");
        }
        Set<Long> actividadIds = new HashSet<>(request.actividadIds() == null ? List.of() : request.actividadIds());

        Map<Long, Actividad> actividadesSolicitadas = actividadIds.stream()
                .map(actividadId -> actividadRepository.findById(actividadId)
                        .filter(encontrada -> encontrada.getClub() != null && encontrada.getClub().getId().equals(clubId))
                        .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada")))
                .peek(actividad -> validarInscripcionPermitida(clubId, socio, actividad))
                .collect(Collectors.toMap(Actividad::getId, Function.identity()));

        List<InscripcionActividad> existentes = inscripcionActividadRepository.findByClubIdAndSocioIdAndEstado(clubId, socioId, EstadoInscripcion.ACTIVA);
        existentes.stream()
                .filter(inscripcion -> !actividadIds.contains(inscripcion.getActividad().getId()))
                .forEach(inscripcion -> {
                    inscripcion.setEstado(EstadoInscripcion.BAJA);
                    inscripcion.setFechaBaja(LocalDate.now());
                    inscripcion.setUsuarioResponsable(usuarioResponsable);
                    inscripcion.setObservaciones(request.observaciones());
                    inscripcionActividadRepository.save(inscripcion);
                });

        for (Long actividadId : actividadIds) {
            Actividad actividad = actividadesSolicitadas.get(actividadId);
            InscripcionActividad inscripcion = inscripcionActividadRepository.findByClubIdAndSocioIdAndActividadId(clubId, socioId, actividadId)
                    .orElseGet(InscripcionActividad::new);
            inscripcion.setClub(club);
            inscripcion.setSocio(socio);
            inscripcion.setActividad(actividad);
            inscripcion.setFechaInscripcion(inscripcion.getFechaInscripcion() == null ? LocalDate.now() : inscripcion.getFechaInscripcion());
            inscripcion.setEstado(EstadoInscripcion.ACTIVA);
            inscripcion.setFechaBaja(null);
            inscripcion.setUsuarioResponsable(usuarioResponsable);
            inscripcion.setObservaciones(request.observaciones());
            inscripcionActividadRepository.save(inscripcion);
        }

        auditoriaService.registrar(usuarioResponsable, clubId, "MODIFICACION", "INSCRIPCION", socioId,
                "Actividades activas asignadas: " + actividadIds.size());
        return listarActividadesDeSocio(clubId, socioId);
    }

    private void validarInscripcionPermitida(Long clubId, Socio socio, Actividad actividad) {
        if (!"ACTIVO".equals(socio.getEstado())) {
            throw new IllegalArgumentException("No se puede inscribir un socio inactivo");
        }
        if (actividad.getEstado() != EstadoActividad.ACTIVA) {
            throw new IllegalArgumentException("No se puede inscribir en una actividad inactiva");
        }

        boolean yaActiva = inscripcionActividadRepository
                .findByClubIdAndSocioIdAndActividadId(clubId, socio.getId(), actividad.getId())
                .filter(inscripcion -> inscripcion.getEstado() == EstadoInscripcion.ACTIVA)
                .isPresent();
        long totalActivas = inscripcionActividadRepository
                .findByClubIdAndActividadIdAndEstado(clubId, actividad.getId(), EstadoInscripcion.ACTIVA)
                .stream()
                .filter(inscripcion -> inscripcionValidaParaClub(inscripcion, clubId))
                .count();
        if (!yaActiva && totalActivas >= actividad.getCupo()) {
            throw new IllegalArgumentException("La actividad no tiene cupos disponibles");
        }
    }

    @Transactional
    public ActividadResponse crearActividad(Long clubId, CrearActividadRequest request, Usuario actor) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));

        Actividad actividad = new Actividad();
        actividad.setClub(club);
        actividad.setNombre(request.nombre());
        actividad.setProfesorUsuario(validarProfesorDelClub(clubId, request.profesorUsuarioId()));
        actividad.setDias(request.dias());
        actividad.setCategoria(request.categoria());
        actividad.setIcono(request.icono() == null || request.icono().isBlank() ? "check" : request.icono());
        actividad.setCupo(request.cupo());
        actividad.setInscriptos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);

        Actividad guardada = actividadRepository.save(actividad);
        auditoriaService.registrar(actor, clubId, "ALTA", "ACTIVIDAD", guardada.getId(), "Actividad creada");
        return actividadResponse(guardada);
    }

    @Transactional
    public ActividadResponse actualizarActividad(Long clubId, Long actividadId, ActualizarActividadRequest request, Usuario actor) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .filter(encontrada -> encontrada.getClub().getId().equals(clubId))
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        actividad.setNombre(request.nombre());
        actividad.setProfesorUsuario(validarProfesorDelClub(clubId, request.profesorUsuarioId()));
        actividad.setDias(request.dias());
        actividad.setCategoria(request.categoria());
        actividad.setIcono(request.icono() == null || request.icono().isBlank() ? "check" : request.icono());
        actividad.setCupo(request.cupo());
        actividad.setEstado(request.estado());

        Actividad guardada = actividadRepository.save(actividad);
        auditoriaService.registrar(actor, clubId, "MODIFICACION", "ACTIVIDAD", actividadId,
                "Actividad actualizada; estado=" + request.estado());
        return actividadResponse(guardada);
    }

    private Usuario validarProfesorDelClub(Long clubId, Long profesorUsuarioId) {
        return usuarioClubRepository.findByUsuarioIdAndClubId(profesorUsuarioId, clubId)
                .filter(asignacion -> asignacion.getRol() == RolClub.PROFESOR)
                .map(asignacion -> asignacion.getUsuario())
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.ACTIVO)
                .filter(usuario -> usuario.getRol() == RolUsuario.PROFESOR)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un profesor creado y activo para este club"));
    }

    public List<AsistenciaResponse> listarAsistencia(Long clubId, Long actividadId, LocalDate fecha) {
        Map<Long, Asistencia> asistenciasPorSocio = asistenciaRepository.findByClubIdAndActividadIdAndFecha(clubId, actividadId, fecha).stream()
                .collect(Collectors.toMap(asistencia -> asistencia.getSocio().getId(), Function.identity()));

        return inscripcionActividadRepository.findByClubIdAndActividadIdAndEstado(clubId, actividadId, EstadoInscripcion.ACTIVA).stream()
                .filter(inscripcion -> inscripcionValidaParaClub(inscripcion, clubId))
                .map(InscripcionActividad::getSocio)
                .map(socio -> {
                    Asistencia asistencia = asistenciasPorSocio.get(socio.getId());
                    return asistencia == null ? AsistenciaResponse.sinRegistro(actividadId, socio, fecha) : AsistenciaResponse.desde(asistencia);
                })
                .toList();
    }

    @Transactional
    public List<AsistenciaResponse> guardarAsistencia(
            Long clubId,
            Long actividadId,
            LocalDate fecha,
            List<GuardarAsistenciaRequest> presentes,
            boolean restringirAFechaActual,
            Usuario usuarioResponsable
    ) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Actividad actividad = actividadRepository.findById(actividadId)
                .filter(encontrada -> encontrada.getClub().getId().equals(clubId))
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        validarFechaDeAsistencia(actividad, fecha, restringirAFechaActual);

        List<AsistenciaResponse> resultado = presentes.stream().map(item -> {
            Socio socio = socioRepository.findById(item.socioId())
                    .orElseThrow(() -> new SocioNoEncontradoException(item.socioId()));
            if (socio.getClub() == null || !socio.getClub().getId().equals(clubId)) {
                throw new IllegalArgumentException("El socio no pertenece al club");
            }
            inscripcionActividadRepository.findByClubIdAndSocioIdAndActividadId(clubId, item.socioId(), actividadId)
                    .filter(inscripcion -> inscripcion.getEstado() == EstadoInscripcion.ACTIVA)
                    .orElseThrow(() -> new IllegalArgumentException("El socio no esta inscripto en la actividad"));
            Asistencia asistencia = asistenciaRepository.findByClubIdAndActividadIdAndFecha(clubId, actividadId, fecha).stream()
                    .filter(existente -> existente.getSocio().getId().equals(item.socioId()))
                    .findFirst()
                    .orElseGet(Asistencia::new);

            asistencia.setClub(club);
            asistencia.setActividad(actividad);
            asistencia.setSocio(socio);
            asistencia.setFecha(fecha);
            asistencia.setEstado(item.estadoEfectivo());
            asistencia.setUsuarioResponsable(usuarioResponsable);
            asistencia.setFechaActualizacion(LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")));
            return AsistenciaResponse.desde(asistenciaRepository.save(asistencia));
        }).toList();
        auditoriaService.registrar(usuarioResponsable, clubId, "REGISTRO", "ASISTENCIA", actividadId,
                "Fecha=" + fecha + "; registros=" + resultado.size());
        return resultado;
    }

    private void validarFechaDeAsistencia(Actividad actividad, LocalDate fecha, boolean restringirAFechaActual) {
        if (!esDiaDeClase(actividad.getDias(), fecha.getDayOfWeek())) {
            throw new IllegalArgumentException("La asistencia solo se puede guardar en dias configurados para la actividad");
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        if (restringirAFechaActual && !fecha.equals(hoy)) {
            throw new IllegalArgumentException("Los profesores solo pueden guardar asistencia el dia de la clase");
        }
    }

    private boolean esDiaDeClase(String dias, DayOfWeek diaSemana) {
        String texto = normalizar(dias);
        return switch (diaSemana) {
            case MONDAY -> texto.contains("lunes");
            case TUESDAY -> texto.contains("martes");
            case WEDNESDAY -> texto.contains("miercoles");
            case THURSDAY -> texto.contains("jueves");
            case FRIDAY -> texto.contains("viernes");
            case SATURDAY -> texto.contains("sabado");
            case SUNDAY -> texto.contains("domingo");
        };
    }

    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    private ActividadResponse actividadResponse(Actividad actividad) {
        int inscriptos = (int) inscripcionActividadRepository.countByClubIdAndActividadIdAndEstado(
                actividad.getClub().getId(), actividad.getId(), EstadoInscripcion.ACTIVA
        );
        return ActividadResponse.desde(actividad, inscriptos);
    }

    private boolean inscripcionValidaParaClub(InscripcionActividad inscripcion, Long clubId) {
        if (inscripcion.getClub() == null || inscripcion.getSocio() == null || inscripcion.getActividad() == null) {
            return false;
        }
        return inscripcion.getClub().getId().equals(clubId)
                && inscripcion.getSocio().getClub() != null
                && inscripcion.getSocio().getClub().getId().equals(clubId)
                && inscripcion.getActividad().getClub() != null
                && inscripcion.getActividad().getClub().getId().equals(clubId);
    }

}
