package com.redclubes.backend.socios;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
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

    @PostMapping("/api/socios")
    public Socio crearSocio(@Valid @RequestBody Socio socio) {
        return socioService.crearSocio(socio);
    }

    @GetMapping("/api/socios/{id}")
    public Socio obtenerSocioPorId(@PathVariable Long id) {
        return socioService.obtenerSocioPorId(id);
    }

    @PutMapping("/api/socios/{id}")
    public Socio actualizarSocio(
        @PathVariable Long id,
        @Valid @RequestBody Socio socioActualizado
    ) {
        return socioService.actualizarSocio(id, socioActualizado);
    }   

    @DeleteMapping("/api/socios/{id}")
    public Socio eliminarSocio(@PathVariable Long id) {
        return socioService.eliminarSocio(id);
    }
}