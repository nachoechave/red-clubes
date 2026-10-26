package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redclubes.backend.usuarios.SessionTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
public class NetWebhookController {
    private record Window(Instant until, int count) {}
    private final Map<String, Window> limits = new HashMap<>();
    private final NetPagosService service;
    private final NetPagosConfig config;
    private final ObjectMapper json;
    private final SessionTokenService tokens;
    private final Clock clock;
    NetWebhookController(NetPagosService service, NetPagosConfig config, ObjectMapper json, SessionTokenService tokens, Clock clock) {
        this.service = service; this.config = config; this.json = json; this.tokens = tokens; this.clock = clock;
    }
    @PostMapping(value = "/api/integraciones/netpagos/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> receive(HttpServletRequest request) throws java.io.IOException {
        if (!config.webhookEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        allowed(request.getRemoteAddr());
        byte[] bytes = request.getInputStream().readNBytes(16385);
        if (bytes.length > 16384) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        com.fasterxml.jackson.databind.JsonNode payload;
        try { payload = json.readTree(bytes); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON invalido"); }
        if (payload == null || !payload.isObject()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON invalido");
        service.webhook(payload);
        // Local acknowledgement only. NET must confirm the required success code/retry contract.
        return ResponseEntity.accepted().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
    private synchronized void allowed(String ip) {
        Instant now = clock.instant(); limits.entrySet().removeIf(e -> !e.getValue().until().isAfter(now));
        String key = tokens.hash(ip); var old = limits.get(key);
        if ((old == null && limits.size() >= 10000) || (old != null && old.count() >= 60))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        limits.put(key, new Window(old == null ? now.plusSeconds(900) : old.until(), old == null ? 1 : old.count() + 1));
    }
}
