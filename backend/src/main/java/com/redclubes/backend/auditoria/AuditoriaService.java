package com.redclubes.backend.auditoria;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AuditoriaService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private final AuditoriaRepository auditoriaRepository;
    private final ClubRepository clubRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, ClubRepository clubRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.clubRepository = clubRepository;
    }

    @Transactional
    public void registrar(Usuario actor, Long clubId, String accion, String tipoEntidad, Long entidadId, String detalle) {
        if (actor == null) {
            throw new IllegalArgumentException("La auditoria requiere un usuario responsable");
        }
        Club club = clubId == null ? null : clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(actor);
        auditoria.setClub(club);
        auditoria.setAccion(limitar(accion, 50));
        auditoria.setTipoEntidad(limitar(tipoEntidad, 80));
        auditoria.setEntidadId(entidadId);
        auditoria.setFecha(LocalDateTime.now(BUSINESS_ZONE));
        auditoria.setDetalle(limitar(detalle, 1000));
        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(Long clubId) {
        return auditoriaRepository.findTop100ByClubIdOrderByFechaDesc(clubId).stream()
                .map(AuditoriaResponse::desde)
                .toList();
    }

    private String limitar(String value, int maximo) {
        if (value == null) {
            return null;
        }
        String limpio = value.replace('\r', ' ').replace('\n', ' ').trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
