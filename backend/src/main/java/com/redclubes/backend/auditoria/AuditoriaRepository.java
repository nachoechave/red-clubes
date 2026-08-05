package com.redclubes.backend.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findTop100ByClubIdOrderByFechaDesc(Long clubId);
}
