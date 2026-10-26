package com.redclubes.backend.netpagos;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class NetWebhookEvent {
    @Id @Column(length = 64) String fingerprint;
    @Column(nullable = false, length = 36) String intentId;
    @Column(nullable = false, length = 30) String reportedStatus;
    @Column(nullable = false) Instant receivedAt;
    // Deliberately no payer, document, email, API key or raw request body.
    protected NetWebhookEvent() {}
}
