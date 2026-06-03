package com.redclubes.backend.gestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findByClubIdAndActividadIdAndFecha(Long clubId, Long actividadId, LocalDate fecha);

    List<Asistencia> findByClubId(Long clubId);

    boolean existsByClubIdAndActividadIdAndSocioIdAndFecha(Long clubId, Long actividadId, Long socioId, LocalDate fecha);
}
