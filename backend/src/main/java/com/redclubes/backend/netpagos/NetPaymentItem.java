package com.redclubes.backend.netpagos;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class NetPaymentItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 36) String intentId;
    @Column(nullable = false) Long cuotaId;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount;
    @Column(nullable = false, length = 128) String barcode;
    @Column(nullable = false, length = 80) String service;
    @Column(nullable = false, length = 120) String detail;
    Long pagoId;
    protected NetPaymentItem() {}
}
