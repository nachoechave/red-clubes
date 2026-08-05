package com.redclubes.backend.clubes;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.AccesoDenegadoException;
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
            throw new AccesoDenegadoException();
        }

        if (clubRepository.existsByNombre(request.nombre())) {
            throw new IllegalArgumentException("Ya existe un club con ese nombre");
        }

        Club club = new Club(request.nombre(), request.direccion(), EstadoClub.ACTIVO);
        club.setLogoUrl(normalizarLogo(request.logoUrl()));
        return ClubResponse.desde(clubRepository.save(club));
    }

    public ClubResponse actualizarClub(String authorizationHeader, Long clubId, ActualizarClubRequest request) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorizationHeader);
        if (usuario.getRol() != RolUsuario.SUPERUSUARIO) {
            throw new AccesoDenegadoException();
        }

        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        clubRepository.findByNombre(request.nombre())
                .filter(clubExistente -> !clubExistente.getId().equals(clubId))
                .ifPresent(clubExistente -> {
                    throw new IllegalArgumentException("Ya existe un club con ese nombre");
                });

        club.setNombre(request.nombre());
        club.setDireccion(request.direccion());
        club.setLogoUrl(normalizarLogo(request.logoUrl()));
        club.setEstado(request.estado());

        return ClubResponse.desde(clubRepository.save(club));
    }

    private String normalizarLogo(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        if (!logoUrl.startsWith("data:image/")) {
            throw new IllegalArgumentException("El logo debe ser una imagen valida");
        }
        return logoUrl;
    }
}
