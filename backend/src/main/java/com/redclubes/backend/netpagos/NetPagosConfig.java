package com.redclubes.backend.netpagos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class NetPagosConfig {
    public enum Mode { OFF, SIMULATED, STAGING }
    final Mode mode;
    final String apiKey;
    final long clubId;
    final boolean webhookEnabled;
    final boolean settlementEnabled;
    final Long actorId;

    public NetPagosConfig(@Value("${netpagos.mode:OFF}") Mode mode,
            @Value("${netpagos.api-key:}") String apiKey,
            @Value("${portal.club-id:0}") long clubId,
            @Value("${netpagos.webhook-enabled:false}") boolean webhookEnabled,
            @Value("${netpagos.settlement-enabled:false}") boolean settlementEnabled,
            @Value("${netpagos.contract-confirmed:false}") boolean contractConfirmed,
            @Value("${netpagos.actor-id:0}") Long actorId, Environment environment) {
        var profiles = Arrays.asList(environment.getActiveProfiles());
        if (mode != Mode.OFF && (clubId <= 0 || !profiles.contains("netpagos-sandbox") || profiles.contains("prod")))
            throw new IllegalArgumentException("NET Pagos solo se prepara en perfil netpagos-sandbox, sin prod y con institucion configurada");
        if (mode == Mode.STAGING && (apiKey.isBlank() || apiKey.contains("\r") || apiKey.contains("\n") || apiKey.length() > 512))
            throw new IllegalArgumentException("STAGING requiere API key valida");
        if (settlementEnabled && (mode == Mode.OFF || actorId <= 0 || (mode == Mode.STAGING && !contractConfirmed)))
            throw new IllegalArgumentException("Acreditacion requiere actor y contrato confirmado (excepto simulacion)");
        if (webhookEnabled && mode == Mode.OFF) throw new IllegalArgumentException("Webhook requiere modo de ensayo");
        this.mode = mode; this.apiKey = apiKey; this.clubId = clubId;
        this.webhookEnabled = webhookEnabled; this.settlementEnabled = settlementEnabled; this.actorId = actorId;
    }
}
