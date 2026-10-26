package com.redclubes.backend.portal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class PortalChallenge {
    @Id @Column(length = 36) String id;
    Long socioId;
    @Column(nullable = false) Long clubId;
    @Column(length = 36) String revision;
    @Column(nullable = false, length = 64) String codeHash;
    @Column(nullable = false) Instant expiresAt;
    int attempts;
    boolean consumed;
    protected PortalChallenge() {}
}
