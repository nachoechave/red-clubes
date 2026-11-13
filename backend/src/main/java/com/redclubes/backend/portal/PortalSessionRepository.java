package com.redclubes.backend.portal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface PortalSessionRepository extends JpaRepository<PortalSession, String> {
    long deleteByExpiresAtBefore(Instant now);
}
