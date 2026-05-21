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
        return socioRepository.findById(id).orElse(null);
    }
}