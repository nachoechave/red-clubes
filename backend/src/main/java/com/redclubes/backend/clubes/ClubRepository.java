package com.redclubes.backend.clubes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Club c where c.id = :id")
    Optional<Club> findByIdForUpdate(Long id);
    boolean existsByNombre(String nombre);

    Optional<Club> findByNombre(String nombre);
}
