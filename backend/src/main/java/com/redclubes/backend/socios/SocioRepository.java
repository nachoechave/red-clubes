package com.redclubes.backend.socios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SocioRepository extends JpaRepository<Socio, Long> {
    java.util.List<Socio> findByClubId(Long clubId);

    java.util.List<Socio> findByClubIdOrderByIdAsc(Long clubId);

    java.util.Optional<Socio> findByIdAndClubId(Long id, Long clubId);

    boolean existsByClubIdAndDni(Long clubId, String dni);

    boolean existsByClubIdAndDniAndIdNot(Long clubId, String dni, Long id);

    @Query("select coalesce(max(s.numeroSocio), 0) from Socio s where s.club.id = :clubId")
    int maxNumeroSocioPorClub(Long clubId);

    boolean existsByDni(String dni);

    java.util.Optional<Socio> findByDni(String dni);
}
