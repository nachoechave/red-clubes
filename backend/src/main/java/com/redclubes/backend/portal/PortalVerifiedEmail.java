package com.redclubes.backend.portal;

import jakarta.persistence.*;
import java.time.Instant;

/** Independent evidence: a populated Socio.email is never sufficient. */
@Entity
public class PortalVerifiedEmail {
    @Id Long socioId;
    @Column(nullable = false) Long clubId;
    @Column(nullable = false) String email;
    @Column(nullable = false, length = 36) String revision;
    @Column(nullable = false) Instant verifiedAt;
    @Column(nullable = false) Long verifiedBy;
    @Column(nullable = false) String evidence;
    boolean revoked;
    protected PortalVerifiedEmail() {}
}
