package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByClubIdAndCuotaIdAndEstado(Long clubId, Long cuotaId, EstadoPago estado);
    List<Pago> findByClubIdAndCuotaIdOrderByIdDesc(Long clubId, Long cuotaId);
    Optional<Pago> findByIdAndClubIdAndCuotaId(Long id, Long clubId, Long cuotaId);
    List<Pago> findByClubIdAndEstadoAndFechaPagoGreaterThanEqualAndFechaPagoLessThan(
            Long clubId, EstadoPago estado, LocalDateTime desde, LocalDateTime hasta
    );
}
