package com.redclubes.backend.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Clock;

@Configuration
public class PortalConfig {
    final boolean enabled;
    final long clubId;
    final String secret;
    final int sessionMinutes;
    final int attempts;
    final int identityRequests;
    final int ipRequests;
    final int cooldownSeconds;

    public PortalConfig(@Value("${portal.enabled:false}") boolean enabled,
                        @Value("${portal.club-id:0}") long clubId,
                        @Value("${portal.secret:}") String secret,
                        @Value("${portal.session-minutes:15}") int sessionMinutes,
                        @Value("${portal.max-attempts:5}") int attempts,
                        @Value("${portal.identity-requests:3}") int identityRequests,
                        @Value("${portal.ip-requests:20}") int ipRequests,
                        @Value("${portal.cooldown-seconds:60}") int cooldownSeconds) {
        if (enabled && (clubId <= 0 || secret.length() < 32))
            throw new IllegalArgumentException("Portal requiere institucion y secreto aleatorio de al menos 32 caracteres");
        if (sessionMinutes < 1 || sessionMinutes > 60 || attempts < 1 || attempts > 10
                || identityRequests < 1 || ipRequests < 1 || cooldownSeconds < 1)
            throw new IllegalArgumentException("Politicas del portal invalidas");
        this.enabled = enabled; this.clubId = clubId; this.secret = secret;
        this.sessionMinutes = sessionMinutes; this.attempts = attempts;
        this.identityRequests = identityRequests; this.ipRequests = ipRequests;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Bean Clock portalClock() { return Clock.systemUTC(); }

    @Bean(name = "portalMailExecutor")
    ThreadPoolTaskExecutor portalMailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("portal-mail-");
        return executor;
    }
}
