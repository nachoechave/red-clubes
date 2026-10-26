package com.redclubes.backend.netpagos;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NetPaymentReservationRepository extends JpaRepository<NetPaymentReservation, Long> {
    void deleteByIntentId(String id);
}
