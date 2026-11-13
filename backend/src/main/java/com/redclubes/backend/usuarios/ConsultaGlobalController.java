package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint exclusivamente de lectura para consultas institucionales.
 * No reutiliza permisos de OPERADOR ni abre los endpoints de escritura por club.
 */
@RestController
@RequestMapping("/api/consulta-global")
public class ConsultaGlobalController {
    private final AuthService auth;
    private final ClubRepository clubes;
    private final SocioRepository socios;
    private final JdbcTemplate jdbc;

    public ConsultaGlobalController(AuthService auth, ClubRepository clubes, SocioRepository socios, JdbcTemplate jdbc) {
        this.auth=auth;
        this.clubes=clubes;
        this.socios=socios;
        this.jdbc=jdbc;
    }

    public record ResumenClub(Long id, String nombre, long sociosTotales, long sociosActivos) {}
    public record SocioConsulta(Long id, Long clubId, String nombre, String apellido, String dni,
                                Integer numeroSocio, String estado, String telefono, String email,
                                String direccion, java.time.LocalDate fechaNacimiento, java.time.LocalDate fechaAlta,
                                int mesesPagadosHistoricos, int mesesPendientesHistoricos) {}

    private void autorizar(String authorization) {
        RolUsuario rol=auth.obtenerUsuarioAutenticado(authorization).getRol();
        if (rol != RolUsuario.CONSULTOR_GLOBAL && rol != RolUsuario.SUPERUSUARIO) {
            throw new AccesoDenegadoException();
        }
    }

    @GetMapping("/clubes")
    public List<ResumenClub> resumen(@RequestHeader("Authorization") String authorization) {
        autorizar(authorization);
        Map<Long,List<Socio>> porClub = socios.findAll().stream().filter(s -> s.getClub()!=null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getClub().getId()));
        return clubes.findAll().stream().map(c -> {
            List<Socio> lista=porClub.getOrDefault(c.getId(),List.of());
            return new ResumenClub(c.getId(),c.getNombre(),lista.size(),
                    lista.stream().filter(s -> "ACTIVO".equals(s.getEstado())).count());
        }).toList();
    }

    @GetMapping("/socios")
    public List<SocioConsulta> consultarSocios(@RequestHeader("Authorization") String authorization,
                                                @RequestParam(required=false) Long clubId) {
        autorizar(authorization);
        if (clubId != null && !clubes.existsById(clubId)) {
            throw new IllegalArgumentException("Institución inexistente");
        }
        List<Socio> lista=clubId==null? socios.findAll() : socios.findByClubId(clubId);
        // Resumen de estados históricos declarados; no representa deuda monetaria.
        return lista.stream().map(s -> {
            List<Map<String,Object>> historial=jdbc.queryForList(
                    "SELECT estado, COUNT(*) cantidad FROM historial_cuota_importada "+
                    "WHERE club_id=? AND socio_id=? GROUP BY estado",s.getClub().getId(),s.getId());
            int pagos=0, pendientes=0;
            for(Map<String,Object> estado:historial) {
                int cantidad=((Number)estado.get("cantidad")).intValue();
                if("PAGADO".equals(estado.get("estado")))pagos+=cantidad;
                if("PENDIENTE".equals(estado.get("estado")))pendientes+=cantidad;
            }
            return new SocioConsulta(s.getId(),s.getClub().getId(),s.getNombre(),s.getApellido(),
                    s.getDni(),s.getNumeroSocio(),s.getEstado(),s.getTelefono(),s.getEmail(),
                    s.getDireccion(),s.getFechaNacimiento(),s.getFechaAlta(),pagos,pendientes);
        }).toList();
    }
}
