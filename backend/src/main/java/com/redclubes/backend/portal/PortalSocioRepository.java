package com.redclubes.backend.portal;

import com.redclubes.backend.socios.Socio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import java.util.Optional;

public interface PortalSocioRepository extends Repository<Socio, Long> {
    Optional<Socio> findByClubIdAndDni(Long clubId, String dni);
    Optional<Socio> findByIdAndClubId(Long id, Long clubId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Socio s where s.id = :id and s.club.id = :clubId")
    Optional<Socio> lock(Long id, Long clubId);
}
