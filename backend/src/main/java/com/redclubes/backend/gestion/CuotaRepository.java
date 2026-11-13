package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    @Query(value = "select count(*) from net_payment_reservation where cuota_id = :id", nativeQuery = true)
    long contarReservasNetPagos(Long id);
    List<Cuota> findByClubIdAndSocioIdOrderByVencimientoDesc(Long clubId, Long socioId);
    List<Cuota> findByClubId(Long clubId);

    Optional<Cuota> findByIdAndClubId(Long id, Long clubId);

    boolean existsByClubIdAndSocioIdAndMes(Long clubId, Long socioId, String mes);

    boolean existsByClubIdAndSocioIdAndPeriodo(Long clubId, Long socioId, String periodo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuota c where c.id = :id and c.club.id = :clubId")
    Optional<Cuota> findLockedByIdAndClubId(@Param("id") Long id, @Param("clubId") Long clubId);
}
