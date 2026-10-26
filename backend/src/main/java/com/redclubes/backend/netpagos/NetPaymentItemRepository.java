package com.redclubes.backend.netpagos;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NetPaymentItemRepository extends JpaRepository<NetPaymentItem, Long> {
    List<NetPaymentItem> findByIntentIdOrderByCuotaIdAsc(String id);
}
