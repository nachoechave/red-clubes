package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.gestion.Actividad;
import com.redclubes.backend.gestion.ActividadRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final UsuarioActividadRepository usuarioActividadRepository;
    private final ClubRepository clubRepository;
    private final ActividadRepository actividadRepository;
    private final PasswordService passwordService;
    private final AuditoriaService auditoriaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            UsuarioActividadRepository usuarioActividadRepository,
            ClubRepository clubRepository,
            ActividadRepository actividadRepository,
            PasswordService passwordService,
            AuditoriaService auditoriaService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.usuarioActividadRepository = usuarioActividadRepository;
        this.clubRepository = clubRepository;
        this.actividadRepository = actividadRepository;
        this.passwordService = passwordService;
        this.auditoriaService = auditoriaService;
    }

    public List<UsuarioResponse> listarUsuarios(Usuario usuarioAutenticado) {
        Set<Long> clubesAdministrados = clubesAdministrados(usuarioAutenticado);
        return usuarioRepository.findAll()
                .stream()
                .filter(usuario -> usuarioAutenticado.getRol() == RolUsuario.SUPERUSUARIO
                        || usuarioClubRepository.findByUsuarioId(usuario.getId()).stream()
                        .anyMatch(asignacion -> clubesAdministrados.contains(asignacion.getClub().getId())))
                .map(this::usuarioResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request, Usuario usuarioAutenticado) {
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese DNI");
        }
        if (request.rol() == RolUsuario.SUPERUSUARIO && usuarioAutenticado.getRol() != RolUsuario.SUPERUSUARIO) {
            throw new AccesoDenegadoException();
        }

        List<AsignacionUsuarioRequest> asignaciones = normalizarAsignaciones(request.asignaciones(), request.clubId(), request.rolClub());
        if (request.rol() != RolUsuario.SUPERUSUARIO && asignaciones.isEmpty()) {
            throw new IllegalArgumentException("Los usuarios no superusuarios deben tener club y rol por club");
        }
        if (asignaciones.stream().anyMatch(asignacion -> asignacion == null
                || asignacion.clubId() == null || asignacion.rolClub() == null)) {
            throw new IllegalArgumentException("Cada asignacion debe indicar club y rol");
        }
        long clubesDistintos = asignaciones.stream().map(AsignacionUsuarioRequest::clubId).distinct().count();
        if (clubesDistintos != asignaciones.size()) {
            throw new IllegalArgumentException("No se puede repetir un club en las asignaciones");
        }
        asignaciones.stream()
                .filter(asignacion -> asignacion.clubId() != null && asignacion.rolClub() != null)
                .forEach(asignacion -> validarPuedeAsignar(usuarioAutenticado, asignacion));

        Usuario usuario = usuarioRepository.save(new Usuario(
                request.dni(),
                request.nombre(),
                request.apellido(),
                request.rol(),
                EstadoUsuario.ACTIVO,
                passwordService.generarHash(request.passwordInicial()),
                true
        ));

        if (request.rol() != RolUsuario.SUPERUSUARIO) {
            guardarAsignaciones(usuario, asignaciones, usuarioAutenticado);
        }

        Set<Long> clubesAuditados = asignaciones.stream().map(AsignacionUsuarioRequest::clubId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        auditarPorClub(usuarioAutenticado, usuario, "ALTA", "Usuario creado con rol=" + request.rol(), clubesAuditados);

        return usuarioResponse(usuario);
    }

    public UsuarioResponse desactivarUsuario(Long id, Usuario usuarioAutenticado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            throw new IllegalArgumentException("No se puede desactivar el superusuario");
        }

        exigirControlTotalSobreUsuario(usuarioAutenticado, usuario);

        usuario.setEstado(EstadoUsuario.INACTIVO);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        auditarPorClub(usuarioAutenticado, usuarioGuardado, "BAJA", "Usuario desactivado", clubesDeUsuario(usuario));
        return usuarioResponse(usuarioGuardado);
    }

    public UsuarioResponse activarUsuario(Long id, Usuario usuarioAutenticado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        usuario.setEstado(EstadoUsuario.ACTIVO);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        auditarPorClub(usuarioAutenticado, usuarioGuardado, "ACTIVACION", "Usuario activado", clubesDeUsuario(usuario));
        return usuarioResponse(usuarioGuardado);
    }

    @Transactional
    public UsuarioResponse actualizarAsignaciones(Long id, ActualizarAsignacionesUsuarioRequest request, Usuario usuarioAutenticado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            throw new IllegalArgumentException("El superusuario no requiere asignaciones");
        }

        Set<Long> clubesAuditados = new HashSet<>(clubesDeUsuario(usuario));
        List<AsignacionUsuarioRequest> nuevas = request.asignaciones() == null ? List.of() : request.asignaciones();
        nuevas.stream().map(AsignacionUsuarioRequest::clubId).filter(java.util.Objects::nonNull).forEach(clubesAuditados::add);
        guardarAsignaciones(usuario, nuevas, usuarioAutenticado);
        auditarPorClub(usuarioAutenticado, usuario, "PERMISOS", "Asignaciones actualizadas", clubesAuditados);
        return usuarioResponse(usuario);
    }

    private Set<Long> clubesDeUsuario(Usuario usuario) {
        return usuarioClubRepository.findByUsuarioId(usuario.getId()).stream()
                .map(asignacion -> asignacion.getClub().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void auditarPorClub(Usuario actor, Usuario objetivo, String accion, String detalle, Set<Long> clubes) {
        if (clubes.isEmpty()) {
            auditoriaService.registrar(actor, null, accion, "USUARIO", objetivo.getId(), detalle);
            return;
        }
        clubes.forEach(clubId -> auditoriaService.registrar(actor, clubId, accion, "USUARIO", objetivo.getId(), detalle));
    }

    private void guardarAsignaciones(Usuario usuario, List<AsignacionUsuarioRequest> asignaciones, Usuario usuarioAutenticado) {
        asignaciones.stream()
                .filter(asignacion -> asignacion.clubId() != null && asignacion.rolClub() != null)
                .forEach(asignacion -> validarPuedeAsignar(usuarioAutenticado, asignacion));

        if (usuarioAutenticado != null && usuarioAutenticado.getRol() != RolUsuario.SUPERUSUARIO) {
            Set<Long> clubesAdministrados = clubesAdministrados(usuarioAutenticado);
            boolean conservaClubAjeno = usuarioClubRepository.findByUsuarioId(usuario.getId()).stream()
                    .anyMatch(asignacion -> !clubesAdministrados.contains(asignacion.getClub().getId()));
            if (conservaClubAjeno) {
                throw new AccesoDenegadoException();
            }
        }

        usuarioActividadRepository.deleteByUsuarioId(usuario.getId());
        usuarioClubRepository.deleteByUsuarioId(usuario.getId());

        for (AsignacionUsuarioRequest asignacion : asignaciones) {
            if (asignacion.clubId() == null || asignacion.rolClub() == null) {
                continue;
            }
            Club club = clubRepository.findById(asignacion.clubId())
                    .orElseThrow(() -> new ClubNoEncontradoException(asignacion.clubId()));
            usuarioClubRepository.save(new UsuarioClub(usuario, club, asignacion.rolClub()));

            if (asignacion.rolClub() == RolClub.PROFESOR) {
                Set<Long> actividadIds = new HashSet<>(asignacion.actividadIds() == null ? List.of() : asignacion.actividadIds());
                for (Long actividadId : actividadIds) {
                    Actividad actividad = actividadRepository.findById(actividadId)
                            .filter(encontrada -> encontrada.getClub().getId().equals(club.getId()))
                            .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada para el club asignado"));
                    usuarioActividadRepository.save(new UsuarioActividad(usuario, club, actividad));
                }
            }
        }
    }

    private void validarPuedeAsignar(Usuario usuarioAutenticado, AsignacionUsuarioRequest asignacion) {
        if (usuarioAutenticado != null && usuarioAutenticado.getRol() == RolUsuario.SUPERUSUARIO) {
            return;
        }
        if (usuarioAutenticado == null) {
            throw new AccesoDenegadoException();
        }
        usuarioClubRepository.findByUsuarioIdAndClubId(usuarioAutenticado.getId(), asignacion.clubId())
                .filter(usuarioClub -> usuarioClub.getRol() == RolClub.ADMINISTRADOR)
                .orElseThrow(AccesoDenegadoException::new);
        if (asignacion.rolClub() == RolClub.ADMINISTRADOR) {
            throw new AccesoDenegadoException();
        }
    }

    private List<AsignacionUsuarioRequest> normalizarAsignaciones(List<AsignacionUsuarioRequest> asignaciones, Long clubId, RolClub rolClub) {
        if (asignaciones != null && !asignaciones.isEmpty()) {
            return asignaciones;
        }
        if (clubId == null || rolClub == null) {
            return List.of();
        }
        List<AsignacionUsuarioRequest> resultado = new ArrayList<>();
        resultado.add(new AsignacionUsuarioRequest(clubId, rolClub, List.of()));
        return resultado;
    }

    private Set<Long> clubesAdministrados(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return Set.of();
        }
        return usuarioClubRepository.findByUsuarioId(usuario.getId()).stream()
                .filter(asignacion -> asignacion.getRol() == RolClub.ADMINISTRADOR)
                .map(asignacion -> asignacion.getClub().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void exigirControlTotalSobreUsuario(Usuario usuarioAutenticado, Usuario usuarioObjetivo) {
        if (usuarioAutenticado.getRol() == RolUsuario.SUPERUSUARIO) {
            return;
        }
        Set<Long> clubesAdministrados = clubesAdministrados(usuarioAutenticado);
        List<UsuarioClub> asignacionesObjetivo = usuarioClubRepository.findByUsuarioId(usuarioObjetivo.getId());
        if (asignacionesObjetivo.isEmpty()
                || asignacionesObjetivo.stream().anyMatch(asignacion -> !clubesAdministrados.contains(asignacion.getClub().getId()))) {
            throw new AccesoDenegadoException();
        }
    }

    private UsuarioResponse usuarioResponse(Usuario usuario) {
        return UsuarioResponse.desde(
                usuario,
                usuarioClubRepository.findByUsuarioId(usuario.getId()),
                usuarioActividadRepository.findByUsuarioId(usuario.getId())
        );
    }
}
