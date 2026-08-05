package com.redclubes.backend.gestion;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/clubes/{clubId}/cuotas")
public class CobranzaController {
    private final CobranzaService cobranzaService;
    private final AuthService authService;

    public CobranzaController(CobranzaService cobranzaService, AuthService authService) {
        this.cobranzaService = cobranzaService;
        this.authService = authService;
    }

    @GetMapping
    public List<CuotaResponse> listar(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long clubId,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long socioId
    ) {
        authService.exigirAccesoAClub(authorization, clubId);
        if (periodo != null && !periodo.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new IllegalArgumentException("El periodo debe tener formato YYYY-MM");
        }
        EstadoCuota estadoCuota;
        try {
            estadoCuota = estado == null ? null : EstadoCuota.valueOf(estado);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Estado de cuota invalido");
        }
        return cobranzaService.listarCuotas(clubId, periodo, estadoCuota, socioId);
    }

    @PostMapping
    public CuotaResponse crear(@RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
                               @Valid @RequestBody CrearCuotaRequest request) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorization);
        authService.exigirAdministradorDeClub(authorization, clubId);
        return cobranzaService.crearCuota(clubId, request, actor);
    }

    @PostMapping("/generacion")
    public GeneracionCuotasResponse generar(@RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
                                            @Valid @RequestBody GenerarCuotasRequest request) {
        Usuario actor = authService.obtenerUsuarioAutenticado(authorization);
        authService.exigirAdministradorDeClub(authorization, clubId);
        return cobranzaService.generarCuotas(clubId, request, actor);
    }

    @PostMapping("/{cuotaId}/pagos")
    public PagoResponse pagar(@RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
                              @PathVariable Long cuotaId, @Valid @RequestBody RegistrarPagoRequest request) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorization);
        authService.exigirAdministradorDeClub(authorization, clubId);
        return cobranzaService.registrarPago(clubId, cuotaId, request, usuario);
    }

    @GetMapping("/{cuotaId}/pagos")
    public List<PagoResponse> pagos(@RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
                                    @PathVariable Long cuotaId) {
        authService.exigirAccesoAClub(authorization, clubId);
        return cobranzaService.listarPagos(clubId, cuotaId);
    }

    @PostMapping("/{cuotaId}/pagos/{pagoId}/anulacion")
    public PagoResponse anular(@RequestHeader("Authorization") String authorization, @PathVariable Long clubId,
                               @PathVariable Long cuotaId, @PathVariable Long pagoId,
                               @Valid @RequestBody AnularPagoRequest request) {
        Usuario usuario = authService.obtenerUsuarioAutenticado(authorization);
        authService.exigirAdministradorDeClub(authorization, clubId);
        return cobranzaService.anularPago(clubId, cuotaId, pagoId, request, usuario);
    }
}
