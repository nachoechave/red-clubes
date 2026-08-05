package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionActividadRepository extends JpaRepository<InscripcionActividad, Long> {
    List<InscripcionActividad> findByClubIdAndSocioIdAndEstado(Long clubId, Long socioId, EstadoInscripcion estado);

    List<InscripcionActividad> findByClubIdAndActividadIdAndEstado(Long clubId, Long actividadId, EstadoInscripcion estado);

    Optional<InscripcionActividad> findByClubIdAndSocioIdAndActividadId(Long clubId, Long socioId, Long actividadId);

    long countByClubIdAndActividadIdAndEstado(Long clubId, Long actividadId, EstadoInscripcion estado);
}
