package com.redclubes.backend.socios;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocioRepository extends JpaRepository<Socio, Long> {
    java.util.List<Socio> findByClubId(Long clubId);

    boolean existsByDni(String dni);

    java.util.Optional<Socio> findByDni(String dni);
}
