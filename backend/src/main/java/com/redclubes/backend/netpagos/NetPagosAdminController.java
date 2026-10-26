package com.redclubes.backend.netpagos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/netpagos-administracion")
public class NetPagosAdminController {
    public record Barcode(@NotNull @Pattern(regexp = "[0-9]{1,128}") String barcode,
                          @NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,80}") String service,
                          @NotNull LocalDate validoHasta, @NotBlank @Size(max = 200) String referenciaConvenio) {}
    public record Simulation(@NotNull String estado, @NotNull @Size(min = 1, max = 10) List<@NotNull Boolean> paid) {}
    public record Recovery(@NotNull UUID preorderUuid) {}
    private final NetPagosService service;
    NetPagosAdminController(NetPagosService service) { this.service = service; }
    @GetMapping("/intenciones")
    NetPagosService.PageView queue(@RequestHeader("Authorization") String header,
            @RequestParam(defaultValue = "REVIEW") NetPaymentIntent.State estado, @RequestParam(defaultValue = "0") int pagina) {
        return service.reviewQueue(header, estado, pagina);
    }
    @PostMapping("/cuotas/{cuotaId}/barra")
    ResponseEntity<Void> barcode(@RequestHeader("Authorization") String header, @PathVariable Long cuotaId,
                                 @Valid @RequestBody Barcode body) {
        service.registerBarcode(header, cuotaId, body.barcode(), body.service(), body.validoHasta(), body.referenciaConvenio());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/intenciones/{id}/conciliar")
    NetPagosService.View refresh(@RequestHeader("Authorization") String header, @PathVariable UUID id) {
        return service.adminRefresh(header, id.toString());
    }
    @PostMapping("/intenciones/{id}/recuperar")
    NetPagosService.View recover(@RequestHeader("Authorization") String header, @PathVariable UUID id,
                                @Valid @RequestBody Recovery body) {
        return service.recover(header, id.toString(), body.preorderUuid().toString());
    }
    @PostMapping("/intenciones/{id}/simular")
    ResponseEntity<Void> simulate(@RequestHeader("Authorization") String header, @PathVariable UUID id,
                                  @Valid @RequestBody Simulation body) {
        service.simulate(header, id.toString(), body.estado(), body.paid());
        return ResponseEntity.noContent().build();
    }
}
