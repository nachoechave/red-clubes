package com.redclubes.backend.usuarios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;
    private final SessionTokenService tokenService;

    public LoginAttemptService(
            @Value("${redclubes.auth.login.max-attempts:5}") int maxAttempts,
            @Value("${redclubes.auth.login.window-minutes:15}") long windowMinutes,
            @Value("${redclubes.auth.login.block-minutes:15}") long blockMinutes,
            SessionTokenService tokenService
    ) {
        if (maxAttempts < 1 || windowMinutes < 1 || blockMinutes < 1) {
            throw new IllegalArgumentException("La proteccion de login requiere valores positivos");
        }
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
        this.blockDuration = Duration.ofMinutes(blockMinutes);
        this.tokenService = tokenService;
    }

    public String key(String identity, String clientAddress) {
        String normalizedIdentity = identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
        String normalizedAddress = clientAddress == null ? "unknown" : clientAddress.trim();
        return tokenService.hash(normalizedIdentity + "|" + normalizedAddress);
    }

    public void verifyAllowed(String key) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }
        Instant now = Instant.now();
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            throw new CredencialesInvalidasException();
        }
        if (state.windowStarted.plus(window).isBefore(now)) {
            attempts.remove(key, state);
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            AttemptState state = current;
            if (state == null || state.windowStarted.plus(window).isBefore(now)) {
                state = new AttemptState(now, 0, null);
            }
            int failures = state.failures + 1;
            Instant blockedUntil = failures >= maxAttempts ? now.plus(blockDuration) : state.blockedUntil;
            return new AttemptState(state.windowStarted, failures, blockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    @Scheduled(cron = "${redclubes.auth.login.cleanup-cron:0 */15 * * * *}")
    public void cleanup() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            return (state.blockedUntil == null || !state.blockedUntil.isAfter(now))
                    && state.windowStarted.plus(window).isBefore(now);
        });
    }

    private record AttemptState(Instant windowStarted, int failures, Instant blockedUntil) {
    }
}
