package com.redclubes.backend.clubes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByNombre(String nombre);

    Optional<Club> findByNombre(String nombre);
}
