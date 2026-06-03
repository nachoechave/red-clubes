package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findByClubId(Long clubId);

    long countByClubIdAndEstado(Long clubId, EstadoActividad estado);

    Optional<Actividad> findByClubIdAndNombre(Long clubId, String nombre);
}
