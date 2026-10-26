package com.redclubes.backend.portal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.Optional;

public interface PortalChallengeRepository extends JpaRepository<PortalChallenge, String> {
    @Query("select c.socioId from PortalChallenge c where c.id = :id")
    Optional<Long> findSocioId(String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PortalChallenge c where c.id = :id")
    Optional<PortalChallenge> findLockedById(String id);
    @Modifying
    @Query("update PortalChallenge c set c.consumed = true where c.socioId = :socioId")
    void consumeForSocio(Long socioId);
    long deleteByExpiresAtBefore(Instant now);
}
