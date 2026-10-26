package com.redclubes.backend.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal-bomberos-administracion/socios")
public class PortalActivationController {
    public record Activation(@NotBlank @Size(max = 200) String referenciaVerificacion, boolean revocar) {}
    private final PortalService service;
    PortalActivationController(PortalService service) { this.service = service; }
    @PostMapping("/{socioId}/correo-verificado")
    ResponseEntity<Void> activate(@RequestHeader("Authorization") String header, @PathVariable Long socioId,
                                  @Valid @RequestBody Activation body) {
        service.activate(header, socioId, body.referenciaVerificacion(), body.revocar());
        return ResponseEntity.noContent().build();
    }
}
