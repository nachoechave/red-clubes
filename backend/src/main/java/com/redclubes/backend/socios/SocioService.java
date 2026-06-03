package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SocioService {

    private final SocioRepository socioRepository;
    private final ClubRepository clubRepository;

    public SocioService(SocioRepository socioRepository, ClubRepository clubRepository) {
        this.socioRepository = socioRepository;
        this.clubRepository = clubRepository;
    }

    public List<Socio> listarSocios() {
        return socioRepository.findAll();
    }

    public List<Socio> listarSociosPorClub(Long clubId) {
        return socioRepository.findByClubId(clubId);
    }

    public Socio crearSocio(Socio socio) {
        return socioRepository.save(socio);
    }

    public Socio crearSocioEnClub(Long clubId, Socio socio) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNoEncontradoException(clubId));
        socio.setClub(club);
        return socioRepository.save(socio);
    }

    public Socio obtenerSocioPorId(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new SocioNoEncontradoException(id));
    }

    public Socio actualizarSocio(Long id, Socio socioActualizado) {
        Socio socioExistente = socioRepository.findById(id)
                            .orElseThrow(() -> new SocioNoEncontradoException(id));

        socioExistente.setNombre(socioActualizado.getNombre());
        socioExistente.setApellido(socioActualizado.getApellido());
        socioExistente.setDni(socioActualizado.getDni());
        socioExistente.setEstado(socioActualizado.getEstado());
        socioExistente.setTelefono(socioActualizado.getTelefono());
        socioExistente.setDireccion(socioActualizado.getDireccion());
        socioExistente.setEmergenciaNombre(socioActualizado.getEmergenciaNombre());
        socioExistente.setEmergenciaTelefono(socioActualizado.getEmergenciaTelefono());
        socioExistente.setEmergenciaRelacion(socioActualizado.getEmergenciaRelacion());

        return socioRepository.save(socioExistente);
    }

    public Socio eliminarSocio(Long id) {
        Socio socioExistente = socioRepository.findById(id)
                .orElseThrow(() -> new SocioNoEncontradoException(id));

        socioExistente.setEstado("INACTIVO");

        return socioRepository.save(socioExistente);
    }
}
