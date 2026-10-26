package com.redclubes.backend.portal;

import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.clubes.EstadoClub;
import com.redclubes.backend.gestion.*;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.usuarios.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class PortalService {
    public record Principal(Long socioId, Long clubId, String tokenHash) {}
    public record ChallengeResponse(String solicitudId, String mensaje) {}
    public record SessionResponse(String token, String tokenType, Instant expiresAt) {}
    public record MeResponse(String nombre, Integer numeroSocio) {}
    public record CuotaPortal(Long id, String periodo, BigDecimal importe, BigDecimal pagado,
                              BigDecimal saldoPendiente, LocalDate vencimiento, EstadoCuota estado) {}
    private final PortalConfig config;
    private final PortalSocioRepository socios;
    private final PortalVerifiedEmailRepository emails;
    private final PortalChallengeRepository challenges;
    private final PortalSessionRepository sessions;
    private final CuotaRepository cuotas;
    private final CobranzaService cobranza;
    private final PortalCrypto crypto;
    private final SessionTokenService tokens;
    private final PortalRateLimiter limits;
    private final PortalMailService mail;
    private final Clock clock;
    private final AuthService auth;
    private final AuditoriaService audit;

    PortalService(PortalConfig config, PortalSocioRepository socios, PortalVerifiedEmailRepository emails,
                  PortalChallengeRepository challenges, PortalSessionRepository sessions, CuotaRepository cuotas,
                  PortalCrypto crypto, SessionTokenService tokens, PortalRateLimiter limits, PortalMailService mail,
                  Clock clock, AuthService auth, AuditoriaService audit, CobranzaService cobranza) {
        this.config = config; this.socios = socios; this.emails = emails; this.challenges = challenges;
        this.sessions = sessions; this.cuotas = cuotas; this.crypto = crypto; this.tokens = tokens;
        this.limits = limits; this.mail = mail; this.clock = clock; this.auth = auth; this.audit = audit;
        this.cobranza = cobranza;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChallengeResponse requestCode(String dni, String ip) {
        String id = UUID.randomUUID().toString();
        var response = new ChallengeResponse(id, "Si el acceso esta habilitado, recibiras un codigo. Si necesitas ayuda, contacta a la institucion.");
        if (!config.enabled) return response;
        boolean ipAllowed = limits.allow(crypto.hash("request-ip:" + ip), config.ipRequests, 0);
        boolean identityAllowed = limits.allow(crypto.hash("request-dni:" + dni), config.identityRequests, config.cooldownSeconds);
        if (!ipAllowed || !identityAllowed) return response;
        String code = crypto.code();
        var challenge = new PortalChallenge();
        challenge.id = id; challenge.clubId = config.clubId;
        challenge.codeHash = crypto.hash(id + ":" + code);
        challenge.expiresAt = clock.instant().plusSeconds(300);
        Socio socio = socios.findByClubIdAndDni(config.clubId, dni)
                .flatMap(s -> socios.lock(s.getId(), config.clubId)).orElse(null);
        PortalVerifiedEmail verified = socio == null ? null : eligible(socio);
        if (verified != null) {
            challenges.consumeForSocio(socio.getId());
            challenge.socioId = socio.getId(); challenge.revision = verified.revision;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { mail.enqueue(verified.email, code); }
            });
        }
        challenges.save(challenge);
        return response;
    }

    // Authentication failures must commit the attempt counter, including the final failed attempt.
    @Transactional(noRollbackFor = AutenticacionRequeridaException.class, isolation = Isolation.READ_COMMITTED)
    public SessionResponse verify(String id, String code, String ip) {
        if (!config.enabled) throw new AutenticacionRequeridaException();
        if (!limits.allow(crypto.hash("verify-ip:" + ip), 60, 0)) throw new AutenticacionRequeridaException();
        // Read only the scalar locator before acquiring locks. Loading the entity here would
        // leave stale attempts/consumed values in the persistence context during a race.
        Long socioId = challenges.findSocioId(id).orElse(null);
        Socio socio = socioId == null ? null : socios.lock(socioId, config.clubId).orElse(null);
        PortalChallenge challenge = challenges.findLockedById(id).orElseThrow(AutenticacionRequeridaException::new);
        String identity = socio == null ? id : socio.getDni();
        if (!limits.allow(crypto.hash("verify-identity:" + identity), 20, 0)) throw new AutenticacionRequeridaException();
        if (challenge.consumed || challenge.attempts >= config.attempts
                || !challenge.expiresAt.isAfter(clock.instant()) || challenge.clubId != config.clubId)
            throw new AutenticacionRequeridaException();
        challenge.attempts++;
        PortalVerifiedEmail verified = socio == null ? null : eligible(socio);
        boolean matches = crypto.matches(challenge.codeHash, crypto.hash(id + ":" + code));
        if (!matches || verified == null || !verified.revision.equals(challenge.revision))
            throw new AutenticacionRequeridaException();
        challenge.consumed = true;
        String token = tokens.generarToken();
        var session = new PortalSession();
        session.tokenHash = tokens.hash(token); session.socioId = socio.getId(); session.clubId = config.clubId;
        session.revision = verified.revision; session.expiresAt = clock.instant().plusSeconds(config.sessionMinutes * 60L);
        sessions.save(session);
        return new SessionResponse(token, "Bearer", session.expiresAt);
    }

    @Transactional(readOnly = true)
    public Principal authenticate(String header) {
        if (!config.enabled || header == null || !header.matches("Bearer [A-Za-z0-9_-]{43}"))
            throw new AutenticacionRequeridaException();
        String hash = tokens.hash(header.substring(7));
        PortalSession session = sessions.findById(hash).orElseThrow(AutenticacionRequeridaException::new);
        if (session.revoked || session.clubId != config.clubId || !session.expiresAt.isAfter(clock.instant()))
            throw new AutenticacionRequeridaException();
        Socio socio = socios.findByIdAndClubId(session.socioId, config.clubId).orElseThrow(AutenticacionRequeridaException::new);
        PortalVerifiedEmail verified = eligible(socio);
        if (verified == null || !verified.revision.equals(session.revision)) throw new AutenticacionRequeridaException();
        return new Principal(socio.getId(), config.clubId, hash);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Principal principal) {
        Socio socio = socios.findByIdAndClubId(principal.socioId(), principal.clubId()).orElseThrow(AutenticacionRequeridaException::new);
        return new MeResponse(socio.getNombre(), socio.getNumeroSocio());
    }

    @Transactional(readOnly = true)
    public List<CuotaPortal> cuotas(Principal principal) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("America/Argentina/Buenos_Aires")));
        return cuotas.findByClubIdAndSocioIdOrderByVencimientoDesc(principal.clubId(), principal.socioId()).stream()
                .map(c -> {
                    var saldo = cobranza.consultarSaldo(c);
                    return new CuotaPortal(c.getId(), c.getPeriodo(), c.getImporte(), saldo.pagado(), saldo.pendiente(), c.getVencimiento(),
                        c.getEstado() == EstadoCuota.PENDIENTE && c.getVencimiento().isBefore(today)
                                ? EstadoCuota.VENCIDA : c.getEstado());
                }).toList();
    }

    @Transactional
    public void logout(Principal principal) {
        var session = sessions.findById(principal.tokenHash()).orElseThrow(AutenticacionRequeridaException::new);
        session.revoked = true;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void activate(String header, Long socioId, String evidence, boolean revoke) {
        if (!config.enabled) throw new AccesoDenegadoException();
        auth.exigirAdministradorDeClub(header, config.clubId);
        var actor = auth.obtenerUsuarioAutenticado(header);
        var socio = socios.lock(socioId, config.clubId).orElseThrow(() -> new IllegalArgumentException("Socio no disponible"));
        if (!revoke && (socio.getEmail() == null || !socio.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
            throw new IllegalArgumentException("Se requiere un correo valido registrado");
        var verified = emails.findById(socioId).orElseGet(PortalVerifiedEmail::new);
        verified.socioId = socioId; verified.clubId = config.clubId;
        verified.email = socio.getEmail() == null ? "" : socio.getEmail();
        verified.revision = UUID.randomUUID().toString(); verified.verifiedAt = clock.instant();
        verified.verifiedBy = actor.getId(); verified.evidence = evidence; verified.revoked = revoke;
        emails.save(verified);
        challenges.consumeForSocio(socioId);
        audit.registrar(actor, config.clubId, revoke ? "REVOCACION_PORTAL" : "ACTIVACION_PORTAL", "SOCIO", socioId,
                "Verificacion asistida; referencia=" + evidence);
    }

    private PortalVerifiedEmail eligible(Socio socio) {
        if (!"ACTIVO".equals(socio.getEstado()) || socio.getClub().getEstado() != EstadoClub.ACTIVO) return null;
        return emails.findById(socio.getId()).filter(v -> !v.revoked && v.clubId == config.clubId
                && v.verifiedAt != null && v.email.equals(socio.getEmail())).orElse(null);
    }

    @Scheduled(fixedDelayString = "${portal.cleanup-ms:600000}")
    @Transactional
    public void cleanup() {
        challenges.deleteByExpiresAtBefore(clock.instant());
        sessions.deleteByExpiresAtBefore(clock.instant());
    }
}
