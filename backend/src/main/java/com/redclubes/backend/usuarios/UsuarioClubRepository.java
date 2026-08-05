package com.redclubes.backend.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioClubRepository extends JpaRepository<UsuarioClub, Long> {
    List<UsuarioClub> findByUsuarioId(Long usuarioId);

    List<UsuarioClub> findByClubIdAndRol(Long clubId, RolClub rol);

    Optional<UsuarioClub> findByUsuarioIdAndClubId(Long usuarioId, Long clubId);

    boolean existsByUsuarioIdAndClubId(Long usuarioId, Long clubId);

    void deleteByUsuarioId(Long usuarioId);
}
