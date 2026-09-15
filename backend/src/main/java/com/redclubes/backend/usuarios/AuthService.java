package com.redclubes.backend.usuarios;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final UsuarioActividadRepository usuarioActividadRepository;
    private final PasswordService passwordService;
    private final SessionTokenService sessionTokenService;
    private final LoginAttemptService loginAttemptService;
    private final long sessionHours;

    public AuthService(
            UsuarioRepository usuarioRepository,
            SesionUsuarioRepository sesionUsuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            UsuarioActividadRepository usuarioActividadRepository,
            PasswordService passwordService,
            SessionTokenService sessionTokenService,
            LoginAttemptService loginAttemptService,
            @Value("${redclubes.auth.session-hours}") long sessionHours
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.usuarioActividadRepository = usuarioActividadRepository;
        this.passwordService = passwordService;
        this.sessionTokenService = sessionTokenService;
        this.loginAttemptService = loginAttemptService;
        this.sessionHours = sessionHours;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientAddress) {
        String attemptKey = loginAttemptService.key(request.dni(), clientAddress);
        loginAttemptService.verifyAllowed(attemptKey);
        Usuario usuario;
        try {
            usuario = usuarioRepository.findByDni(request.dni())
                    .filter(usuarioEncontrado -> usuarioEncontrado.getEstado() == EstadoUsuario.ACTIVO)
                    .filter(usuarioEncontrado -> passwordService.coincide(request.password(), usuarioEncontrado.getPasswordHash()))
                    .orElseThrow(CredencialesInvalidasException::new);
        } catch (CredencialesInvalidasException exception) {
            loginAttemptService.recordFailure(attemptKey);
            throw exception;
        }
        loginAttemptService.recordSuccess(attemptKey);

        if (passwordService.necesitaRehash(usuario.getPasswordHash())) {
            usuario.setPasswordHash(passwordService.generarHash(request.password()));
            usuarioRepository.save(usuario);
        }

        String token = sessionTokenService.generarToken();
        SesionUsuario sesion = new SesionUsuario(
                sessionTokenService.hash(token),
                LocalDateTime.now().plusHours(sessionHours),
                usuario
        );

        sesionUsuarioRepository.save(sesion);

        return new LoginResponse(token, usuarioResponse(usuario));
    }

    public Usuario obtenerUsuarioAutenticado(String authorizationHeader) {
        String token = extraerToken(authorizationHeader);
        SesionUsuario sesion = sesionUsuarioRepository.findByTokenHash(sessionTokenService.hash(token))
                .filter(sesionEncontrada -> sesionEncontrada.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(AutenticacionRequeridaException::new);

        if (sesion.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new AutenticacionRequeridaException();
        }

        return sesion.getUsuario();
    }

    public UsuarioResponse me(String authorizationHeader) {
        return usuarioResponse(obtenerUsuarioAutenticado(authorizationHeader));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extraerToken(authorizationHeader);
        String tokenHash = sessionTokenService.hash(token);
        if (sesionUsuarioRepository.findByTokenHash(tokenHash).isEmpty()) {
            throw new AutenticacionRequeridaException();
        }
        sesionUsuarioRepository.deleteByTokenHash(tokenHash);
    }

    @Scheduled(cron = "${redclubes.auth.session-cleanup-cron:0 0 * * * *}")
    @Transactional
    public void eliminarSesionesExpiradas() {
        sesionUsuarioRepository.deleteByFechaExpiracionBefore(LocalDateTime.now());
    }

    public Usuario exigirAdministrador(String authorizationHeader) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (usuario.getRol() != RolUsuario.SUPERUSUARIO && usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AccesoDenegadoException();
        }

        return usuario;
    }

    public void exigirAccesoAClub(String authorizationHeader, Long clubId) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return;
        }

        if (!usuarioClubRepository.existsByUsuarioIdAndClubId(usuario.getId(), clubId)) {
            throw new AccesoDenegadoException();
        }
    }

    public void exigirAdministradorDeClub(String authorizationHeader, Long clubId) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (esAdministradorDeClub(usuario, clubId)) {
            return;
        }

        throw new AccesoDenegadoException();
    }

    public void exigirOperadorDeClub(String authorizationHeader, Long clubId) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        if (puedeOperarClub(usuario, clubId)) {
            return;
        }

        throw new AccesoDenegadoException();
    }

    public boolean esAdministradorDeClub(Usuario usuario, Long clubId) {
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return true;
        }

        return usuarioClubRepository.findByUsuarioIdAndClubId(usuario.getId(), clubId)
                .map(usuarioClub -> usuarioClub.getRol() == RolClub.ADMINISTRADOR)
                .orElse(false);
    }

    public boolean puedeOperarClub(Usuario usuario, Long clubId) {
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return true;
        }

        return usuarioClubRepository.findByUsuarioIdAndClubId(usuario.getId(), clubId)
                .map(usuarioClub -> usuarioClub.getRol() == RolClub.ADMINISTRADOR
                        || usuarioClub.getRol() == RolClub.OPERADOR)
                .orElse(false);
    }

    public void exigirAccesoAActividad(Usuario usuario, Long clubId, Long actividadId) {
        if (puedeOperarClub(usuario, clubId)) {
            return;
        }

        if (usuarioActividadRepository.existsByUsuarioIdAndClubIdAndActividadId(usuario.getId(), clubId, actividadId)) {
            return;
        }

        throw new AccesoDenegadoException();
    }

    public List<Long> actividadesPermitidas(Usuario usuario, Long clubId) {
        if (puedeOperarClub(usuario, clubId)) {
            return List.of();
        }

        return usuarioActividadRepository.findByUsuarioIdAndClubId(usuario.getId(), clubId)
                .stream()
                .map(usuarioActividad -> usuarioActividad.getActividad().getId())
                .toList();
    }

    public UsuarioResponse cambiarPassword(String authorizationHeader, CambiarPasswordRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);

        if (!passwordService.coincide(request.passwordActual(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        if (passwordService.coincide(request.nuevaPassword(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contrasena no puede ser igual a la actual");
        }

        usuario.setPasswordHash(passwordService.generarHash(request.nuevaPassword()));
        usuario.setDebeCambiarPassword(false);

        return usuarioResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse actualizarPerfil(String authorizationHeader, ActualizarPerfilRequest request) {
        Usuario usuario = obtenerUsuarioAutenticado(authorizationHeader);
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());

        return usuarioResponse(usuarioRepository.save(usuario));
    }

    private String extraerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AutenticacionRequeridaException();
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new AutenticacionRequeridaException();
        }
        return token;
    }

    public UsuarioResponse usuarioResponse(Usuario usuario) {
        return UsuarioResponse.desde(
                usuario,
                usuarioClubRepository.findByUsuarioId(usuario.getId()),
                usuarioActividadRepository.findByUsuarioId(usuario.getId())
        );
    }
}
