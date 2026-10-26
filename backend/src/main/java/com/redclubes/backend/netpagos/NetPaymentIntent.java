package com.redclubes.backend.netpagos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class NetPaymentIntent {
    public enum State { CREATING, READY, UNKNOWN, PROCESSING, CONFIRMED, PARTIAL, CANCELLED, REVIEW, APPLIED }
    @Id @Column(length = 36) String id;
    @Column(nullable = false) Long clubId;
    @Column(nullable = false) Long socioId;
    @Column(nullable = false, length = 36) String requestKey;
    @Column(nullable = false, length = 64) String requestHash;
    @Column(nullable = false, length = 64) String payerHash;
    @Column(nullable = false, length = 1) String gender;
    @Column(unique = true, length = 36) String providerUuid;
    @Column(length = 2048) String checkoutUrl;
    @Column(nullable = false) String providerMode;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) State state;
    @Column(nullable = false) Instant createdAt;
    Instant nextQueryAt;
    @Column(length = 80) String issue;
    protected NetPaymentIntent() {}
}
