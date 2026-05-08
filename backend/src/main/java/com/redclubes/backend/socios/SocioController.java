package com.redclubes.backend.socios;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SocioController {

    private final SocioService socioService;

    public SocioController(SocioService socioService) {
        this.socioService = socioService;
    }

    @GetMapping("/api/socios")
    public List<Socio> listarSocios() {
        return socioService.listarSocios();
    }
}