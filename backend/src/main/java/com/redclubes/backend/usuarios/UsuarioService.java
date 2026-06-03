package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final ClubRepository clubRepository;
    private final PasswordService passwordService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            ClubRepository clubRepository,
            PasswordService passwordService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.clubRepository = clubRepository;
        this.passwordService = passwordService;
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuario -> UsuarioResponse.desde(usuario, usuarioClubRepository.findByUsuarioId(usuario.getId())))
                .toList();
    }

    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese DNI");
        }

        if (request.rol() != RolUsuario.SUPERUSUARIO && (request.clubId() == null || request.rolClub() == null)) {
            throw new IllegalArgumentException("Los usuarios no superusuarios deben tener club y rol por club");
        }

        Usuario usuario = usuarioRepository.save(new Usuario(
                request.dni(),
                request.nombre(),
                request.apellido(),
                request.rol(),
                EstadoUsuario.ACTIVO,
                passwordService.generarHash(request.passwordInicial()),
                true
        ));

        if (request.clubId() != null && request.rolClub() != null) {
            Club club = clubRepository.findById(request.clubId())
                    .orElseThrow(() -> new ClubNoEncontradoException(request.clubId()));
            usuarioClubRepository.save(new UsuarioClub(usuario, club, request.rolClub()));
        }

        return UsuarioResponse.desde(usuario, usuarioClubRepository.findByUsuarioId(usuario.getId()));
    }

    public UsuarioResponse desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            throw new IllegalArgumentException("No se puede desactivar el superusuario");
        }

        usuario.setEstado(EstadoUsuario.INACTIVO);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return UsuarioResponse.desde(usuarioGuardado, usuarioClubRepository.findByUsuarioId(usuarioGuardado.getId()));
    }
}
