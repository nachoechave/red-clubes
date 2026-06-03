package com.redclubes.backend.usuarios;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final PasswordService passwordService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            SesionUsuarioRepository sesionUsuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            PasswordService passwordService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.passwordService = passwordService;
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByDni(request.dni())
                .filter(usuarioEncontrado -> usuarioEncontrado.getEstado() == EstadoUsuario.ACTIVO)
                .filter(usuarioEncontrado -> passwordService.coincide(request.password(), usuarioEncontrado.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        SesionUsuario sesion = new SesionUsuario(
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(8),
                usuario
        );

        sesionUsuarioRepository.save(sesion);

        return new LoginResponse(sesion.getToken(), usuarioResponse(usuario));
    }

    public Usuario obtenerUsuarioAutenticado(String authorizationHeader) {
        String token = extraerToken(authorizationHeader);
        SesionUsuario sesion = sesionUsuarioRepository.findByToken(token)
                .filter(sesionEncontrada -> sesionEncontrada.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(AccesoDenegadoException::new);

        if (sesion.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new AccesoDenegadoException();
        }

        return sesion.getUsuario();
    }

    public UsuarioResponse me(String authorizationHeader) {
        return usuarioResponse(obtenerUsuarioAutenticado(authorizationHeader));
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
        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return;
        }

        UsuarioClub usuarioClub = usuarioClubRepository.findByUsuarioIdAndClubId(usuario.getId(), clubId)
                .orElseThrow(AccesoDenegadoException::new);

        if (usuarioClub.getRol() != RolClub.ADMINISTRADOR) {
            throw new AccesoDenegadoException();
        }
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
            throw new AccesoDenegadoException();
        }

        return authorizationHeader.substring("Bearer ".length());
    }

    public UsuarioResponse usuarioResponse(Usuario usuario) {
        return UsuarioResponse.desde(usuario, usuarioClubRepository.findByUsuarioId(usuario.getId()));
    }
}
