package com.redclubes.backend.clubes;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.RolUsuario;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.usuarios.UsuarioClubRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final AuthService authService;

    public ClubService(
            ClubRepository clubRepository,
            UsuarioClubRepository usuarioClubRepository,
            AuthService authService
    ) {
        this.clubRepository = clubRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.authService = authService;
    }

    public List<ClubResponse> listarClubesDisponibles(String authorizationHeader) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);

        if (usuario.getRol() == RolUsuario.SUPERUSUARIO) {
            return clubRepository.findAll().stream().map(ClubResponse::desde).toList();
        }

        return usuarioClubRepository.findByUsuarioId(usuario.getId())
                .stream()
                .map(usuarioClub -> ClubResponse.desde(usuarioClub.getClub()))
                .toList();
    }

    public ClubResponse crearClub(String authorizationHeader, CrearClubRequest request) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        if (usuario.getRol() != RolUsuario.SUPERUSUARIO) {
            throw new IllegalArgumentException("Solo el superusuario puede crear clubes");
        }

        if (clubRepository.existsByNombre(request.nombre())) {
            throw new IllegalArgumentException("Ya existe un club con ese nombre");
        }

        Club club = new Club(request.nombre(), request.direccion(), EstadoClub.ACTIVO);
        return ClubResponse.desde(clubRepository.save(club));
    }
}
