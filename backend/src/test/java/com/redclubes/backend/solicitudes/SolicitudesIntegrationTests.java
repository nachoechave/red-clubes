package com.redclubes.backend.solicitudes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redclubes.backend.clubes.*;
import com.redclubes.backend.socios.*;
import com.redclubes.backend.usuarios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"redclubes.solicitudes.publicas.enabled=true", "redclubes.solicitudes.publicas.club-id=900001", "redclubes.solicitudes.publicas.max-intentos=1000"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SolicitudesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClubRepository clubes;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioClubRepository asignaciones;
    @Autowired SesionUsuarioRepository sesiones;
    @Autowired SessionTokenService tokens;
    @Autowired SolicitudRepository solicitudes;
    @Autowired SocioRepository socios;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    Long clubId = 900001L;
    Long otroId;
    DatosSolicitud datos = new DatosSolicitud("Ana", "Prueba", "99111222", "prueba@example.invalid", "2215551234");
    @BeforeEach void preparar() {
        jdbc.update("INSERT INTO club(id,nombre,direccion,estado,nombre_participantes,tipo_institucion) VALUES(900001,'Bomberos API test','Calle test','ACTIVO','SOCIOS','BOMBEROS')");
        jdbc.update("INSERT INTO club_modulo(club_id,modulo) VALUES(900001,'PARTICIPANTES')");
        otroId = clubes.saveAndFlush(new Club("Otra institución API test", "Calle 2", EstadoClub.ACTIVO)).getId();
        usuario("admin", "99110001", RolUsuario.ADMINISTRADOR, RolClub.ADMINISTRADOR, clubId);
        usuario("otro", "99110002", RolUsuario.ADMINISTRADOR, RolClub.ADMINISTRADOR, otroId);
        usuario("operador", "99110003", RolUsuario.OPERADOR, RolClub.OPERADOR, clubId);
        usuario("profesor", "99110004", RolUsuario.PROFESOR, RolClub.PROFESOR, clubId);
    }
    private void usuario(String token, String dni, RolUsuario rol, RolClub rolClub, Long club) {
        var usuario = usuarios.saveAndFlush(new Usuario(dni, "Prueba", token, rol, EstadoUsuario.ACTIVO, "hash-no-usado", false));
        asignaciones.saveAndFlush(new UsuarioClub(usuario, clubes.findById(club).orElseThrow(), rolClub));
        sesiones.saveAndFlush(new SesionUsuario(tokens.hash(token), LocalDateTime.now().plusHours(1), usuario));
    }
    private CrearSolicitudRequest request(String clave) { return new CrearSolicitudRequest(datos, true, "2026-10-v1", clave, ""); }
    private String ruta() { return "/api/public/instituciones/bomberos-ensenada/solicitudes"; }
    private String privada() { return "/api/clubes/" + clubId + "/solicitudes"; }
    private Long enviar() throws Exception {
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(request(UUID.randomUUID().toString())))).andExpect(status().isAccepted());
        return solicitudes.findByClubIdAndEstado(clubId, SolicitudInscripcion.Estado.PENDIENTE, org.springframework.data.domain.Pageable.unpaged()).getContent().get(0).id;
    }
    @Test void recepcionNoCreaSocioNiExponeDatos() throws Exception {
        long antes = socios.count();
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(request(UUID.randomUUID().toString()))))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.datos").doesNotExist()).andExpect(jsonPath("$.id").doesNotExist());
        assertEquals(antes, socios.count());
        assertEquals(1, solicitudes.findByClubIdAndEstado(clubId, SolicitudInscripcion.Estado.PENDIENTE, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
    }
    @Test void reintentosYDniDuplicadosNoDuplican() throws Exception {
        var request = request(UUID.randomUUID().toString());
        for (int i=0;i<2;i++) mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isAccepted());
        enviar();
        assertEquals(1, solicitudes.findByClubIdAndEstado(clubId, SolicitudInscripcion.Estado.PENDIENTE, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
    }
    @Test void mismaClaveConOtrosDatosSeRechaza() throws Exception {
        var request = request(UUID.randomUUID().toString());
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isAccepted());
        var distinto = new CrearSolicitudRequest(new DatosSolicitud("Otro", "Prueba", "99111223", datos.email(), datos.telefono()), true, "2026-10-v1", request.claveEnvio(), "");
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(distinto))).andExpect(status().isBadRequest());
    }
    @Test void aprobarDosVecesCreaUnSoloSocioYPermiteCorregir() throws Exception {
        var id = enviar();
        var corregidos = new DatosSolicitud("Ana María", datos.apellido(), datos.dni(), datos.email(), datos.telefono());
        for (int i=0;i<2;i++) mvc.perform(post(privada()+"/"+id+"/aprobar").header("Authorization","Bearer admin")
                .contentType(APPLICATION_JSON).content(json.writeValueAsString(corregidos)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA")).andExpect(jsonPath("$.socioId").isNumber());
        assertEquals(1, socios.findByClubId(clubId).size());
        assertEquals("Ana María", socios.findByClubId(clubId).get(0).getNombre());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cuota WHERE club_id=900001", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE club_id=900001 AND accion='APROBAR_SOLICITUD'", Integer.class));
    }
    @Test void rechazarNoCreaSocioYNoPermiteAprobarLuego() throws Exception {
        var id = enviar();
        mvc.perform(post(privada()+"/"+id+"/rechazar").header("Authorization","Bearer admin").contentType(APPLICATION_JSON).content("{\"motivo\":\"Datos incompletos\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
        mvc.perform(post(privada()+"/"+id+"/aprobar").header("Authorization","Bearer admin").contentType(APPLICATION_JSON).content(json.writeValueAsString(datos))).andExpect(status().isConflict());
        assertFalse(socios.existsByClubIdAndDni(clubId, datos.dni()));
        enviar(); // Una nueva solicitud después de un rechazo es posible.
    }
    @Test void autorizacionYSeparacionEntreInstituciones() throws Exception {
        var id = enviar();
        mvc.perform(get(privada())).andExpect(status().isUnauthorized());
        for (String token : List.of("otro","operador","profesor")) {
            mvc.perform(get(privada()).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(post(privada()+"/"+id+"/aprobar").header("Authorization","Bearer "+token).contentType(APPLICATION_JSON).content(json.writeValueAsString(datos))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/clubes/"+otroId+"/solicitudes/"+id+"/aprobar").header("Authorization","Bearer otro").contentType(APPLICATION_JSON).content(json.writeValueAsString(datos))).andExpect(status().isNotFound());
        mvc.perform(get(privada()).header("Authorization","Bearer admin")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }
    @Test void validacionConsentimientoInstitucionYMetodos() throws Exception {
        var request = request(UUID.randomUUID().toString());
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(new CrearSolicitudRequest(datos,false,"2026-10-v1",request.claveEnvio(),"")))).andExpect(status().isBadRequest());
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(post(ruta().replace("bomberos-ensenada","otra-institucion")).contentType(APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isNotFound());
        mvc.perform(get(ruta())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/clubes/"+clubId+"/socios").contentType(APPLICATION_JSON).content("{}" )).andExpect(status().isUnauthorized());
    }
    @Test void honeypotNoGuardaSolicitud() throws Exception {
        var r = request(UUID.randomUUID().toString());
        mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(json.writeValueAsString(new CrearSolicitudRequest(datos,true,"2026-10-v1",r.claveEnvio(),"spam")))).andExpect(status().isAccepted());
        assertEquals(0, solicitudes.findByClubIdAndEstado(clubId, SolicitudInscripcion.Estado.PENDIENTE, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
    }

    @Test @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void enviosYAprobacionesConcurrentesNoDuplican() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var inicio = new java.util.concurrent.CountDownLatch(1);
            String contenido = json.writeValueAsString(request(UUID.randomUUID().toString()));
            java.util.concurrent.Callable<Integer> enviar = () -> { inicio.await(); return mvc.perform(post(ruta()).contentType(APPLICATION_JSON).content(contenido)).andReturn().getResponse().getStatus(); };
            var a = executor.submit(enviar); var b = executor.submit(enviar); inicio.countDown();
            assertEquals(202, a.get(20, java.util.concurrent.TimeUnit.SECONDS)); assertEquals(202, b.get(20, java.util.concurrent.TimeUnit.SECONDS));
            var pendientes = solicitudes.findByClubIdAndEstado(clubId, SolicitudInscripcion.Estado.PENDIENTE, org.springframework.data.domain.Pageable.unpaged());
            assertEquals(1, pendientes.getTotalElements()); Long id = pendientes.getContent().get(0).id;
            var aprobarInicio = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> aprobar = () -> { aprobarInicio.await(); return mvc.perform(post(privada()+"/"+id+"/aprobar").header("Authorization","Bearer admin").contentType(APPLICATION_JSON).content(json.writeValueAsString(datos))).andReturn().getResponse().getStatus(); };
            var c = executor.submit(aprobar); var d = executor.submit(aprobar); aprobarInicio.countDown();
            assertEquals(200, c.get(20, java.util.concurrent.TimeUnit.SECONDS)); assertEquals(200, d.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, socios.findByClubId(clubId).size());
        } finally {
            jdbc.update("DELETE FROM auditoria WHERE club_id IN (?,?)",clubId,otroId);
            jdbc.update("DELETE FROM solicitud_inscripcion WHERE club_id=?",clubId);
            jdbc.update("DELETE FROM socio WHERE club_id=?",clubId);
            jdbc.update("DELETE FROM sesion_usuario WHERE usuario_id IN (SELECT id FROM usuario WHERE dni IN ('99110001','99110002','99110003','99110004'))");
            jdbc.update("DELETE FROM usuario_club WHERE club_id IN (?,?)",clubId,otroId);
            jdbc.update("DELETE FROM usuario WHERE dni IN ('99110001','99110002','99110003','99110004')");
            jdbc.update("DELETE FROM club_modulo WHERE club_id IN (?,?)",clubId,otroId);
            jdbc.update("DELETE FROM club WHERE id IN (?,?)",clubId,otroId);
        }
    }
}
