package com.redclubes.backend.socios;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SocioService {

    private final SocioRepository socioRepository;

    public SocioService(SocioRepository socioRepository) {
        this.socioRepository = socioRepository;
    }

    public List<Socio> listarSocios() {
        return socioRepository.findAll();
    }

    public Socio crearSocio(Socio socio) {
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

        return socioRepository.save(socioExistente);
    }

    public Socio eliminarSocio(Long id) {
        Socio socioExistente = socioRepository.findById(id)
                .orElseThrow(() -> new SocioNoEncontradoException(id));

        socioExistente.setEstado("INACTIVO");

        return socioRepository.save(socioExistente);
    }
}