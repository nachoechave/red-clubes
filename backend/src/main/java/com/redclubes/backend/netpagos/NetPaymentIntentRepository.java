package com.redclubes.backend.netpagos;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
public interface NetPaymentIntentRepository extends JpaRepository<NetPaymentIntent, String> {
    Optional<NetPaymentIntent> findByClubIdAndSocioIdAndRequestKey(Long clubId, Long socioId, String key);
    Optional<NetPaymentIntent> findByProviderUuid(String uuid);
    long countBySocioIdAndCreatedAtAfter(Long socioId, Instant since);
    Page<NetPaymentIntent> findByClubIdAndState(Long clubId, NetPaymentIntent.State state, Pageable page);
    @Query("select n from NetPaymentIntent n where n.clubId = :clubId and n.providerUuid is not null and n.state in :states "
            + "and (n.nextQueryAt is null or n.nextQueryAt <= :now) order by n.nextQueryAt asc, n.createdAt asc")
    List<NetPaymentIntent> pending(Long clubId, List<NetPaymentIntent.State> states, Instant now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NetPaymentIntent n where n.id = :id")
    Optional<NetPaymentIntent> lock(String id);
}
