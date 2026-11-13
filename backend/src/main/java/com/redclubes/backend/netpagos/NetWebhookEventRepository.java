package com.redclubes.backend.netpagos;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NetWebhookEventRepository extends JpaRepository<NetWebhookEvent, String> {
    long countByIntentId(String id);
}
