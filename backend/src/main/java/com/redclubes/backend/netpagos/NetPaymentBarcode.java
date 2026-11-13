package com.redclubes.backend.netpagos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class NetPaymentBarcode {
    @Id Long cuotaId;
    @Column(nullable = false) Long clubId;
    @Column(nullable = false, length = 128) String barcode;
    @Column(nullable = false, length = 80) String service;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount;
    @Column(nullable = false) LocalDate validUntil;
    protected NetPaymentBarcode() {}
}
