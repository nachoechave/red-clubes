package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.redclubes.backend.portal.PortalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/portal-bomberos/pagos")
public class NetPagosController {
    public record Create(@NotNull @Size(min = 1, max = 10) List<@NotNull @Positive Long> cuotaIds,
                         @NotNull UUID claveSolicitud, @NotNull @Pattern(regexp = "[123]") String generoDocumento) {
        @JsonAnySetter public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("El navegador no puede definir otros campos del pago");
        }
    }
    private final NetPagosService service;
    NetPagosController(NetPagosService service) { this.service = service; }
    @PostMapping
    ResponseEntity<NetPagosService.View> create(@AuthenticationPrincipal PortalService.Principal principal,
                                               @Valid @RequestBody Create request) {
        return ResponseEntity.accepted().body(service.create(principal, request.cuotaIds(), request.claveSolicitud().toString(), request.generoDocumento()));
    }
    @GetMapping("/{id}")
    NetPagosService.View view(@AuthenticationPrincipal PortalService.Principal principal, @PathVariable UUID id) {
        return service.view(principal, id.toString());
    }
    @PostMapping("/{id}/actualizar")
    NetPagosService.View refresh(@AuthenticationPrincipal PortalService.Principal principal, @PathVariable UUID id) {
        return service.refresh(principal, id.toString());
    }
}
