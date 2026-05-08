package com.redclubes.backend.socios;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SocioController {

    @GetMapping("/api/socios")
    public List<Socio> listarSocios() {
        return List.of(
                new Socio(1L, "Ana María", "Ruiz", "12345678", "ACTIVO"),
                new Socio(2L, "Carlos", "González", "14567890", "ACTIVO"),
                new Socio(3L, "Elena", "Fernández", "16789012", "ACTIVO"),
                new Socio(4L, "Ricardo", "Gómez", "23456789", "MOROSO")
        );
    }
}