package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    List<Cuota> findByClubId(Long clubId);

    long countByClubIdAndEstado(Long clubId, EstadoCuota estado);

    boolean existsByClubIdAndSocioIdAndMes(Long clubId, Long socioId, String mes);
}
