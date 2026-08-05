package com.redclubes.backend.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {
    Optional<SesionUsuario> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    long deleteByFechaExpiracionBefore(LocalDateTime limite);
}
