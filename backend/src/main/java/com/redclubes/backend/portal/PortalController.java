package com.redclubes.backend.portal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/portal-bomberos")
public class PortalController {
    public record RequestCode(@NotNull @Pattern(regexp = "[0-9]{7,10}") String dni) {}
    public record VerifyCode(@NotNull @Pattern(regexp = "[a-f0-9-]{36}") String solicitudId,
                             @NotNull @Pattern(regexp = "[0-9]{6}") String codigo) {}
    private final PortalService service;
    PortalController(PortalService service) { this.service = service; }
    @PostMapping("/auth/solicitar-codigo")
    ResponseEntity<PortalService.ChallengeResponse> request(@Valid @RequestBody RequestCode body, HttpServletRequest request) {
        return ResponseEntity.accepted().body(service.requestCode(body.dni(), request.getRemoteAddr()));
    }
    @PostMapping("/auth/verificar-codigo")
    PortalService.SessionResponse verify(@Valid @RequestBody VerifyCode body, HttpServletRequest request) {
        return service.verify(body.solicitudId(), body.codigo(), request.getRemoteAddr());
    }
    @GetMapping("/me")
    PortalService.MeResponse me(@AuthenticationPrincipal PortalService.Principal principal, HttpServletRequest request) {
        rejectParameters(request);
        return service.me(principal);
    }
    @GetMapping("/cuotas")
    List<PortalService.CuotaPortal> cuotas(@AuthenticationPrincipal PortalService.Principal principal, HttpServletRequest request) {
        rejectParameters(request);
        return service.cuotas(principal);
    }
    @PostMapping("/auth/cerrar-sesion")
    ResponseEntity<Void> logout(@AuthenticationPrincipal PortalService.Principal principal) {
        service.logout(principal); return ResponseEntity.noContent().build();
    }
    private void rejectParameters(HttpServletRequest request) {
        if (!request.getParameterMap().isEmpty()) throw new IllegalArgumentException("Esta ruta no acepta parametros");
    }
}
