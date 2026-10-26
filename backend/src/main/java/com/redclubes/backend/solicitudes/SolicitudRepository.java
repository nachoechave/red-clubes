package com.redclubes.backend.solicitudes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<SolicitudInscripcion, Long> {
    Optional<SolicitudInscripcion> findByClubIdAndClaveEnvio(Long clubId, String clave);
    Optional<SolicitudInscripcion> findByIdAndClubId(Long id, Long clubId);
    boolean existsByClubIdAndDniPendiente(Long clubId, String dni);
    Page<SolicitudInscripcion> findByClubIdAndEstado(Long clubId, SolicitudInscripcion.Estado estado, Pageable page);
}
