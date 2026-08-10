package com.redclubes.backend.config;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.clubes.EstadoClub;
import com.redclubes.backend.gestion.Actividad;
import com.redclubes.backend.gestion.ActividadRepository;
import com.redclubes.backend.gestion.EstadoActividad;
import com.redclubes.backend.usuarios.EstadoUsuario;
import com.redclubes.backend.usuarios.RolClub;
import com.redclubes.backend.usuarios.RolUsuario;
import com.redclubes.backend.usuarios.SesionUsuario;
import com.redclubes.backend.usuarios.SesionUsuarioRepository;
import com.redclubes.backend.usuarios.SessionTokenService;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.usuarios.UsuarioActividad;
import com.redclubes.backend.usuarios.UsuarioActividadRepository;
import com.redclubes.backend.usuarios.UsuarioClub;
import com.redclubes.backend.usuarios.UsuarioClubRepository;
import com.redclubes.backend.usuarios.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleIsolationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClubRepository clubRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioClubRepository usuarioClubRepository;
    @Autowired private UsuarioActividadRepository usuarioActividadRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private SesionUsuarioRepository sesionUsuarioRepository;
    @Autowired private SessionTokenService sessionTokenService;

    private Club clubA;
    private Club clubB;
    private Usuario operadorA;
    private Usuario administradorA;
    private Usuario profesorA;
    private Usuario superusuario;
    private Actividad actividadAsignadaA;

    @BeforeEach
    void prepararEscenario() {
        clubA = clubRepository.save(new Club("Club A", "Calle A 100", EstadoClub.ACTIVO));
        clubB = clubRepository.save(new Club("Club B", "Calle B 200", EstadoClub.ACTIVO));

        operadorA = usuario("10000001", RolUsuario.OPERADOR);
        administradorA = usuario("10000002", RolUsuario.ADMINISTRADOR);
        profesorA = usuario("10000003", RolUsuario.PROFESOR);
        superusuario = usuario("10000004", RolUsuario.SUPERUSUARIO);

        usuarioClubRepository.save(new UsuarioClub(operadorA, clubA, RolClub.OPERADOR));
        usuarioClubRepository.save(new UsuarioClub(administradorA, clubA, RolClub.ADMINISTRADOR));
        usuarioClubRepository.save(new UsuarioClub(profesorA, clubA, RolClub.PROFESOR));

        actividadAsignadaA = actividad("Taller asignado", clubA, profesorA);
        actividad("Taller no asignado", clubA, profesorA);
        actividad("Taller Club B", clubB, null);
        usuarioActividadRepository.save(new UsuarioActividad(profesorA, clubA, actividadAsignadaA));

        sesion("token-operador-a", operadorA);
        sesion("token-administrador-a", administradorA);
        sesion("token-profesor-a", profesorA);
        sesion("token-superusuario", superusuario);
    }

    @Test
    void operadorGestionaSuClubPeroNoPuedeCruzarAlOtro() throws Exception {
        mockMvc.perform(post("/api/clubes/{clubId}/socios", clubA.getId())
                        .header("Authorization", bearer("token-operador-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Maria","apellido":"Perez","dni":"20000001","estado":"ACTIVO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("20000001"));

        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubB.getId())
                        .header("Authorization", bearer("token-operador-a")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void operadorPuedeGestionarCuotasPeroNoUsuariosActividadesNiAuditoria() throws Exception {
        mockMvc.perform(post("/api/clubes/{clubId}/cuotas/generacion", clubA.getId())
                        .header("Authorization", bearer("token-operador-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"periodo":"2026-08","importe":5000.00,"vencimiento":"2026-08-10"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", bearer("token-operador-a")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clubes/{clubId}/auditoria", clubA.getId())
                        .header("Authorization", bearer("token-operador-a")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/clubes/{clubId}/actividades", clubA.getId())
                        .header("Authorization", bearer("token-operador-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Nueva","profesorUsuarioId":1,"dias":"Lunes 10:00","categoria":"Taller","icono":"check","cupo":10}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorSoloVeActividadesAsignadasYNoVeDatosInternos() throws Exception {
        mockMvc.perform(get("/api/clubes/{clubId}/actividades", clubA.getId())
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(actividadAsignadaA.getId()));

        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubA.getId())
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clubes/{clubId}/dashboard", clubA.getId())
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clubes/{clubId}/cuotas", clubA.getId())
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorNoAccedeAOtroClubNiAOtraActividad() throws Exception {
        mockMvc.perform(get("/api/clubes/{clubId}/actividades", clubB.getId())
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isForbidden());

        Long actividadNoAsignada = actividadRepository.findByClubId(clubA.getId()).stream()
                .filter(actividad -> !actividad.getId().equals(actividadAsignadaA.getId()))
                .findFirst().orElseThrow().getId();
        mockMvc.perform(get("/api/clubes/{clubId}/actividades/{actividadId}/asistencias/2026-08-10",
                                clubA.getId(), actividadNoAsignada)
                        .header("Authorization", bearer("token-profesor-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorLocalAdministraSoloSuClub() throws Exception {
        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubA.getId())
                        .header("Authorization", bearer("token-administrador-a")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clubes/{clubId}/auditoria", clubA.getId())
                        .header("Authorization", bearer("token-administrador-a")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubB.getId())
                        .header("Authorization", bearer("token-administrador-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superusuarioPuedeAccederATodosLosClubes() throws Exception {
        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubA.getId())
                        .header("Authorization", bearer("token-superusuario")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clubes/{clubId}/socios", clubB.getId())
                        .header("Authorization", bearer("token-superusuario")))
                .andExpect(status().isOk());
    }

    private Usuario usuario(String dni, RolUsuario rol) {
        return usuarioRepository.save(new Usuario(
                dni, "Nombre", rol.name(), rol, EstadoUsuario.ACTIVO, "hash-no-utilizado", false
        ));
    }

    private Actividad actividad(String nombre, Club club, Usuario profesor) {
        Actividad actividad = new Actividad();
        actividad.setNombre(nombre);
        actividad.setProfesor(profesor == null ? "Sin asignar" : profesor.getNombre() + " " + profesor.getApellido());
        actividad.setProfesorUsuario(profesor);
        actividad.setDias("Lunes 10:00");
        actividad.setCategoria("Taller");
        actividad.setIcono("check");
        actividad.setCupo(20);
        actividad.setInscriptos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);
        actividad.setClub(club);
        return actividadRepository.save(actividad);
    }

    private void sesion(String token, Usuario usuario) {
        sesionUsuarioRepository.save(new SesionUsuario(
                sessionTokenService.hash(token), LocalDateTime.now().plusHours(1), usuario
        ));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
