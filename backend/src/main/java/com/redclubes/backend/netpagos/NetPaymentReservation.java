package com.redclubes.backend.netpagos;

import jakarta.persistence.*;

@Entity
public class NetPaymentReservation {
    @Id Long cuotaId;
    @Column(nullable = false, length = 36) String intentId;
    protected NetPaymentReservation() {}
}
