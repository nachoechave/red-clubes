package com.redclubes.backend.solicitudes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
public class PublicSolicitudController {
    private final SolicitudService servicio;
    private final int maxIntentos;
    private final Map<String, Ventana> ventanas = new HashMap<>();
    private record Ventana(long inicio, int intentos) {}
    public PublicSolicitudController(SolicitudService servicio,
            @Value("${redclubes.solicitudes.publicas.max-intentos:10}") int maxIntentos) {
        this.servicio = servicio; this.maxIntentos = maxIntentos;
    }
    public static boolean esRutaPublica(String metodo, String ruta) {
        return "POST".equals(metodo) && ruta.matches("/api/public/instituciones/[a-z0-9-]{1,60}/solicitudes");
    }
    @PostMapping("/api/public/instituciones/{slug}/solicitudes")
    public ResponseEntity<Map<String, String>> recibir(@PathVariable String slug,
            @Valid @RequestBody CrearSolicitudRequest request, HttpServletRequest http) {
        limitar(http.getRemoteAddr());
        servicio.recibir(slug, request);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(Map.of("mensaje", "Recibimos tu envío. Si los datos son válidos y no existe una inscripción previa, la institución revisará tu solicitud."));
    }
    private synchronized void limitar(String ip) {
        long ahora = Instant.now().getEpochSecond();
        ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicio() >= 900);
        var ventana = ventanas.getOrDefault(ip, new Ventana(ahora, 0));
        if (ventana.intentos() >= maxIntentos || (!ventanas.containsKey(ip) && ventanas.size() >= 10000)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados envíos. Esperá unos minutos y volvé a intentar.");
        }
        ventanas.put(ip, new Ventana(ventana.inicio(), ventana.intentos() + 1));
    }
}
