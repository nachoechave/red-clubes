package com.redclubes.backend.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioActividadRepository extends JpaRepository<UsuarioActividad, Long> {
    List<UsuarioActividad> findByUsuarioId(Long usuarioId);

    List<UsuarioActividad> findByUsuarioIdAndClubId(Long usuarioId, Long clubId);

    boolean existsByUsuarioIdAndClubIdAndActividadId(Long usuarioId, Long clubId, Long actividadId);

    void deleteByUsuarioId(Long usuarioId);
}
