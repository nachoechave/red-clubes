package com.redclubes.backend.socios;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SocioService {

    public List<Socio> listarSocios() {
        return List.of(
                new Socio(1L, "Ana María", "Ruiz", "12345678", "ACTIVO"),
                new Socio(2L, "Carlos", "González", "14567890", "ACTIVO"),
                new Socio(3L, "Elena", "Fernández", "16789012", "ACTIVO"),
                new Socio(4L, "Ricardo", "Gómez", "23456789", "MOROSO")
        );
    }
}