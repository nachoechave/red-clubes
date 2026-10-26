package com.redclubes.backend.solicitudes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.clubes.*;
import com.redclubes.backend.socios.*;
import com.redclubes.backend.usuarios.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
public class SolicitudService {
    private final SolicitudRepository solicitudes;
    private final ClubRepository clubes;
    private final SocioRepository socios;
    private final SocioService socioService;
    private final AuditoriaService auditoria;
    private final SessionTokenService tokens;
    private final ObjectMapper json;
    private final boolean habilitado;
    private final Long clubPublico;
    private final String slug;

    public SolicitudService(SolicitudRepository solicitudes, ClubRepository clubes, SocioRepository socios,
            SocioService socioService, AuditoriaService auditoria, SessionTokenService tokens, ObjectMapper json,
            @Value("${redclubes.solicitudes.publicas.enabled:false}") boolean habilitado,
            @Value("${redclubes.solicitudes.publicas.club-id:0}") Long clubPublico,
            @Value("${redclubes.solicitudes.publicas.slug:bomberos-ensenada}") String slug) {
        this.solicitudes = solicitudes; this.clubes = clubes; this.socios = socios;
        this.socioService = socioService; this.auditoria = auditoria; this.tokens = tokens;
        this.json = json; this.habilitado = habilitado; this.clubPublico = clubPublico; this.slug = slug;
    }

    @Transactional
    public void recibir(String institucion, CrearSolicitudRequest request) {
        if (!habilitado || clubPublico <= 0 || !slug.equals(institucion)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripciones no disponibles");
        }
        if (request.sitioWeb() != null && !request.sitioWeb().isBlank()) return;
        Club club = bloquearClub(clubPublico);
        if (club.getEstado() != EstadoClub.ACTIVO || !club.tieneModulo(ModuloClub.PARTICIPANTES)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripciones no disponibles");
        }
        String huella;
        try { huella = tokens.hash(json.writeValueAsString(request.datos())); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
        var anterior = solicitudes.findByClubIdAndClaveEnvio(clubPublico, request.claveEnvio());
        if (anterior.isPresent()) {
            if (!anterior.get().huella.equals(huella)) throw new IllegalArgumentException("Usá una nueva referencia para cambiar los datos");
            return;
        }
        // Respuesta pública idéntica: no revela si un DNI ya pertenece al padrón.
        if (socios.existsByClubIdAndDni(clubPublico, request.datos().dni())
                || solicitudes.existsByClubIdAndDniPendiente(clubPublico, request.datos().dni())) return;
        var solicitud = new SolicitudInscripcion();
        solicitud.referencia = UUID.randomUUID().toString(); solicitud.clubId = clubPublico;
        solicitud.claveEnvio = request.claveEnvio(); solicitud.huella = huella;
        solicitud.actualizar(request.datos()); solicitud.dniPendiente = request.datos().dni();
        solicitud.estado = SolicitudInscripcion.Estado.PENDIENTE;
        solicitud.consentimientoVersion = request.consentimientoVersion(); solicitud.fechaCreacion = ahora();
        solicitudes.saveAndFlush(solicitud);
    }

    public record Pagina(List<SolicitudResponse> contenido, int pagina, int totalPaginas, long total) {}
    @Transactional(readOnly = true)
    public Pagina listar(Long clubId, SolicitudInscripcion.Estado estado, int pagina) {
        var result = solicitudes.findByClubIdAndEstado(clubId, estado,
                PageRequest.of(pagina, 20, Sort.by(Sort.Direction.DESC, "fechaCreacion", "id")));
        return new Pagina(result.map(SolicitudResponse::desde).getContent(), pagina, result.getTotalPages(), result.getTotalElements());
    }

    @Transactional
    public SolicitudResponse aprobar(Long clubId, Long id, DatosSolicitud datos, Usuario actor) {
        bloquearClub(clubId);
        var s = buscar(clubId, id);
        if (s.estado == SolicitudInscripcion.Estado.APROBADA) return SolicitudResponse.desde(s);
        exigirPendiente(s);
        var socio = socioService.crearSocioEnClub(clubId,
                new CrearSocioRequest(datos.nombre(), datos.apellido(), datos.dni(), "ACTIVO", datos.telefono(),
                        datos.email(), null, null, null, null, null), actor);
        s.actualizar(datos); s.estado = SolicitudInscripcion.Estado.APROBADA; s.socioId = socio.id();
        finalizar(s, actor);
        auditoria.registrar(actor, clubId, "APROBAR_SOLICITUD", "SOLICITUD", id, "Solicitud aprobada; socio " + socio.id());
        return SolicitudResponse.desde(s);
    }

    @Transactional
    public SolicitudResponse rechazar(Long clubId, Long id, String motivo, Usuario actor) {
        bloquearClub(clubId);
        var s = buscar(clubId, id);
        if (s.estado == SolicitudInscripcion.Estado.RECHAZADA) return SolicitudResponse.desde(s);
        exigirPendiente(s); s.estado = SolicitudInscripcion.Estado.RECHAZADA; s.motivo = motivo.trim();
        finalizar(s, actor);
        auditoria.registrar(actor, clubId, "RECHAZAR_SOLICITUD", "SOLICITUD", id, "Solicitud rechazada");
        return SolicitudResponse.desde(s);
    }

    private Club bloquearClub(Long id) { return clubes.findByIdForUpdate(id).orElseThrow(() -> new ClubNoEncontradoException(id)); }
    private SolicitudInscripcion buscar(Long clubId, Long id) {
        return solicitudes.findByIdAndClubId(id, clubId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }
    private void exigirPendiente(SolicitudInscripcion s) {
        if (s.estado != SolicitudInscripcion.Estado.PENDIENTE) throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue resuelta");
    }
    private void finalizar(SolicitudInscripcion s, Usuario actor) {
        s.dniPendiente = null; s.fechaRevision = ahora(); s.usuarioRevisionId = actor.getId();
        solicitudes.saveAndFlush(s);
    }
    private LocalDateTime ahora() { return LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")); }
}
