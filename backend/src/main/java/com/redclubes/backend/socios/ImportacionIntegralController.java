package com.redclubes.backend.socios;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/clubes/{clubId}/socios/importacion-integral")
public class ImportacionIntegralController {
    private final AuthService auth;
    private final ImportacionIntegralService service;
    public ImportacionIntegralController(AuthService auth, ImportacionIntegralService service) {
        this.auth = auth; this.service = service;
    }

    @PostMapping(value="/vista-previa", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacionIntegralService.Resultado vistaPrevia(
            @RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
            @RequestParam("anio") int anio, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        auth.exigirOperadorDeClub(authorization, clubId);
        return service.procesar(clubId, leer(archivo), anio, false, null);
    }

    @PostMapping(value="/confirmar", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacionIntegralService.Resultado confirmar(
            @RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
            @RequestParam("anio") int anio, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        Usuario actor = auth.obtenerUsuarioAutenticado(authorization);
        auth.exigirOperadorDeClub(authorization, clubId);
        return service.procesar(clubId, leer(archivo), anio, true, actor);
    }

    @GetMapping("/historial")
    public java.util.List<ImportacionIntegralService.Historial> historial(
            @RequestHeader("Authorization") String authorization, @PathVariable Long clubId) {
        auth.exigirOperadorDeClub(authorization, clubId);
        return service.listarHistorial(clubId);
    }

    private String leer(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 1_000_000) {
            throw new IllegalArgumentException("CSV vacio o superior a 1 MB");
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
