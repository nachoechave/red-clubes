package com.redclubes.backend.portal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class PortalSession {
    @Id @Column(length = 64) String tokenHash;
    @Column(nullable = false) Long socioId;
    @Column(nullable = false) Long clubId;
    @Column(nullable = false, length = 36) String revision;
    @Column(nullable = false) Instant expiresAt;
    boolean revoked;
    protected PortalSession() {}
}
