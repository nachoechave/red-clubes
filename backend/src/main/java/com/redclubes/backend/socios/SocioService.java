package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SocioService {

    private final SocioRepository socioRepository;
    private final ClubRepository clubRepository;
    private final AuditoriaService auditoriaService;

    public SocioService(SocioRepository socioRepository, ClubRepository clubRepository, AuditoriaService auditoriaService) {
        this.socioRepository = socioRepository;
        this.clubRepository = clubRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<SocioResponse> listarSociosPorClub(Long clubId) {
        return socioRepository.findByClubIdOrderByIdAsc(clubId).stream()
                .map(SocioResponse::from)
                .toList();
    }

    @Transactional
    public SocioResponse crearSocioEnClub(Long clubId, CrearSocioRequest request, Usuario actor) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Socio socio = new Socio();
        socio.setClub(club);
        aplicarCambios(socio, request.nombre(), request.apellido(), request.dni(), request.estado(),
                request.telefono(), request.email(), request.fechaNacimiento(), request.direccion(),
                request.emergenciaNombre(), request.emergenciaTelefono(), request.emergenciaRelacion());
        validarDniDisponible(clubId, request.dni(), null);
        completarFechaAlta(socio);
        completarNumeroSocio(clubId, socio);
        Socio guardado = socioRepository.save(socio);
        auditoriaService.registrar(actor, clubId, "ALTA", "SOCIO", guardado.getId(), "Socio creado");
        return SocioResponse.from(guardado);
    }

    @Transactional
    public SocioResponse actualizarSocioEnClub(Long clubId, Long id, ActualizarSocioRequest request, Usuario actor) {
        Socio socioExistente = socioRepository.findByIdAndClubId(id, clubId)
                            .orElseThrow(() -> new SocioNoEncontradoException(id));

        validarDniDisponible(clubId, request.dni(), id);
        aplicarCambios(socioExistente, request.nombre(), request.apellido(), request.dni(), request.estado(),
                request.telefono(), request.email(), request.fechaNacimiento(), request.direccion(),
                request.emergenciaNombre(), request.emergenciaTelefono(), request.emergenciaRelacion());

        Socio guardado = socioRepository.save(socioExistente);
        auditoriaService.registrar(actor, clubId, "MODIFICACION", "SOCIO", id, "Datos del socio actualizados");
        return SocioResponse.from(guardado);
    }

    private void completarFechaAlta(Socio socio) {
        if (socio.getFechaAlta() == null) {
            socio.setFechaAlta(LocalDate.now());
        }
    }

    private void completarNumeroSocio(Long clubId, Socio socio) {
        if (socio.getNumeroSocio() == null) {
            socio.setNumeroSocio(socioRepository.maxNumeroSocioPorClub(clubId) + 1);
        }
    }

    @Transactional
    public SocioResponse eliminarSocioEnClub(Long clubId, Long id, Usuario actor) {
        Socio socioExistente = socioRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new SocioNoEncontradoException(id));

        socioExistente.setEstado("INACTIVO");

        Socio guardado = socioRepository.save(socioExistente);
        auditoriaService.registrar(actor, clubId, "BAJA", "SOCIO", id, "Socio marcado como inactivo");
        return SocioResponse.from(guardado);
    }

    private void aplicarCambios(
            Socio socio,
            String nombre,
            String apellido,
            String dni,
            String estado,
            String telefono,
            String email,
            LocalDate fechaNacimiento,
            String direccion,
            String emergenciaNombre,
            String emergenciaTelefono,
            String emergenciaRelacion
    ) {
        socio.setNombre(nombre.trim());
        socio.setApellido(apellido.trim());
        socio.setDni(dni.trim());
        socio.setEstado(estado);
        socio.setTelefono(telefono);
        socio.setEmail(email);
        socio.setFechaNacimiento(fechaNacimiento);
        socio.setDireccion(direccion);
        socio.setEmergenciaNombre(emergenciaNombre);
        socio.setEmergenciaTelefono(emergenciaTelefono);
        socio.setEmergenciaRelacion(emergenciaRelacion);
    }

    private void validarDniDisponible(Long clubId, String dni, Long socioId) {
        boolean duplicado = socioId == null
                ? socioRepository.existsByClubIdAndDni(clubId, dni)
                : socioRepository.existsByClubIdAndDniAndIdNot(clubId, dni, socioId);
        if (duplicado) {
            throw new IllegalArgumentException("El DNI ya se encuentra registrado en el club");
        }
    }
}
