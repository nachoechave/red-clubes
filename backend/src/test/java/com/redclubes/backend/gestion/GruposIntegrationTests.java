package com.redclubes.backend.gestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redclubes.backend.clubes.*;
import com.redclubes.backend.socios.*;
import com.redclubes.backend.usuarios.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GruposIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClubRepository clubes;
    @Autowired SocioRepository socios;
    @Autowired ActividadRepository grupos;
    @Autowired InscripcionActividadRepository inscripciones;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioClubRepository asignaciones;
    @Autowired UsuarioActividadRepository asignacionesGrupo;
    @Autowired SesionUsuarioRepository sesiones;
    @Autowired SessionTokenService tokens;
    Club a, b;
    Socio persona;
    Usuario profesor;

    @BeforeEach void preparar() {
        a = new Club("Flexible A", "Calle A", EstadoClub.ACTIVO);
        a.setModulos(EnumSet.allOf(ModuloClub.class));
        a = clubes.saveAndFlush(a);
        b = new Club("Flexible B", "Calle B", EstadoClub.ACTIVO);
        b.setModulos(EnumSet.allOf(ModuloClub.class));
        b = clubes.saveAndFlush(b);
        persona = new Socio(null, "Juan", "Perez", "40111222", "ACTIVO");
        persona.setClub(a);
        persona = socios.saveAndFlush(persona);
        usuario("admin", "41000001", RolUsuario.ADMINISTRADOR, a, RolClub.ADMINISTRADOR);
        usuario("super", "41000002", RolUsuario.SUPERUSUARIO, null, null);
        profesor = usuario("profesor", "41000003", RolUsuario.PROFESOR, a, RolClub.PROFESOR);
        usuario("operador", "41000004", RolUsuario.OPERADOR, a, RolClub.OPERADOR);
    }

    @ParameterizedTest @EnumSource(TipoGrupo.class)
    void creaCadaTipoSinProfesorDiasNiCupo(TipoGrupo tipo) throws Exception {
        long id = crear(a, tipo, false);
        var grupo = grupos.findById(id).orElseThrow();
        assertEquals(tipo, grupo.getTipo());
        assertNull(grupo.getProfesorUsuario());
        assertNull(grupo.getDias());
        assertNull(grupo.getCupo());
    }

    @Test void inscribeEnCategoriaSinCupoYReportaCantidad() throws Exception {
        long id = crear(a, TipoGrupo.CATEGORIA_DEPORTIVA, false);
        inscribir(id);
        mvc.perform(get(base(a) + "/reportes/grupos?tipo=CATEGORIA_DEPORTIVA").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].inscriptos").value(1));
        inscribir(id);
        assertEquals(1, inscripciones.countByClubIdAndActividadIdAndEstado(a.getId(), id, EstadoInscripcion.ACTIVA));
    }

    @Test void creaActividadConProfesorYMantieneEndpointHistorico() throws Exception {
        mvc.perform(post(base(a) + "/actividades").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content("""
                {"nombre":"Yoga","profesorUsuarioId":%d,"dias":"Lunes","categoria":"Salud","cupo":32}
                """.formatted(profesor.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("ACTIVIDAD"))
                .andExpect(jsonPath("$.profesorUsuarioId").value(profesor.getId()));
        mvc.perform(get(base(a) + "/actividades").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Yoga"));
    }

    @Test void registraAsistenciaSinCalendarioCuandoEstaHabilitada() throws Exception {
        long id = crear(a, TipoGrupo.TALLER, true);
        inscribir(id);
        mvc.perform(post(base(a) + "/grupos/" + id + "/asistencias/2026-09-17")
                .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON)
                .content("[{\"socioId\":" + persona.getId() + ",\"estado\":\"PRESENTE\"}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].presente").value(true));
        mvc.perform(get(base(a) + "/actividades/" + id + "/asistencias/2026-09-17")
                .header("Authorization", "Bearer admin")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presente").value(true));
    }

    @Test void bloqueaAsistenciaDeshabilitadaEnAmbasApis() throws Exception {
        long id = crear(a, TipoGrupo.CATEGORIA_DEPORTIVA, false);
        for (String ruta : new String[]{"grupos", "actividades"}) {
            mvc.perform(get(base(a) + "/" + ruta + "/" + id + "/asistencias/2026-09-17")
                    .header("Authorization", "Bearer admin")).andExpect(status().isBadRequest());
            mvc.perform(post(base(a) + "/" + ruta + "/" + id + "/asistencias/2026-09-17")
                    .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test void bloqueaIdDeOtroClubAunqueLaRutaSeaDelClubAutorizado() throws Exception {
        long id = crear(b, TipoGrupo.TALLER, true);
        mvc.perform(put(base(a) + "/grupos/" + id).header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(payload(TipoGrupo.TALLER, true)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(base(a) + "/grupos/" + id + "/asistencias/2026-09-17")
                .header("Authorization", "Bearer admin")).andExpect(status().isBadRequest());
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/grupos")
                .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON).content("{\"grupoIds\":[" + id + "]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(base(b) + "/grupos").header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
    }

    @Test void convivenTresTiposYSeFiltranSinDuplicarInscripciones() throws Exception {
        long categoria = crear(a, TipoGrupo.CATEGORIA_DEPORTIVA, false);
        long actividad = crear(a, TipoGrupo.ACTIVIDAD, true);
        long taller = crear(a, TipoGrupo.TALLER, true);
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/grupos")
                .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON)
                .content("{\"grupoIds\":[" + categoria + "," + actividad + "," + taller + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        for (TipoGrupo tipo : TipoGrupo.values()) {
            mvc.perform(get(base(a) + "/grupos?tipo=" + tipo).header("Authorization", "Bearer admin"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Test void clienteHistoricoSoloVeActividadesYSuEdicionConservaOtrosTipos() throws Exception {
        long categoria = crear(a, TipoGrupo.CATEGORIA_DEPORTIVA, false);
        long actividad = crear(a, TipoGrupo.ACTIVIDAD, true);
        long taller = crear(a, TipoGrupo.TALLER, true);
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/grupos")
                .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON)
                .content("{\"grupoIds\":[" + categoria + "," + actividad + "," + taller + "]}"))
                .andExpect(status().isOk());
        mvc.perform(get(base(a) + "/actividades").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("ACTIVIDAD"));
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/actividades")
                .header("Authorization", "Bearer admin").contentType(APPLICATION_JSON).content("{\"actividadIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(1, inscripciones.countByClubIdAndActividadIdAndEstado(a.getId(), categoria, EstadoInscripcion.ACTIVA));
        assertEquals(1, inscripciones.countByClubIdAndActividadIdAndEstado(a.getId(), taller, EstadoInscripcion.ACTIVA));
        assertEquals(0, inscripciones.countByClubIdAndActividadIdAndEstado(a.getId(), actividad, EstadoInscripcion.ACTIVA));
    }

    @Test void superusuarioConservaAccesoGlobalYOperadorNoEditaGrupos() throws Exception {
        long id = crear(b, TipoGrupo.TALLER, false);
        mvc.perform(put(base(b) + "/grupos/" + id).header("Authorization", "Bearer super")
                .contentType(APPLICATION_JSON).content(payload(TipoGrupo.TALLER, false))).andExpect(status().isOk());
        mvc.perform(post(base(a) + "/grupos").header("Authorization", "Bearer operador")
                .contentType(APPLICATION_JSON).content(payload(TipoGrupo.TALLER, false))).andExpect(status().isForbidden());
    }

    @Test void profesorSoloVeGruposAsignados() throws Exception {
        long id = crear(a, TipoGrupo.TALLER, false);
        crear(a, TipoGrupo.ACTIVIDAD, true);
        asignacionesGrupo.saveAndFlush(new UsuarioActividad(profesor, a, grupos.findById(id).orElseThrow()));
        mvc.perform(get(base(a) + "/grupos").header("Authorization", "Bearer profesor"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test void configurarClubNoBorraGruposYNecesitaAdministradorDelClub() throws Exception {
        long id = crear(a, TipoGrupo.TALLER, true);
        String config = "{\"modulos\":[\"PARTICIPANTES\",\"CATEGORIAS\"],\"nombreParticipantes\":\"JUGADORES\"}";
        mvc.perform(put(base(a) + "/configuracion").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(config)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreParticipantes").value("JUGADORES"));
        assertTrue(grupos.existsById(id));
        mvc.perform(get(base(a) + "/grupos/" + id + "/asistencias/2026-09-17")
                .header("Authorization", "Bearer admin")).andExpect(status().isBadRequest());
        mvc.perform(put(base(b) + "/configuracion").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(config)).andExpect(status().isForbidden());
    }

    @Test void responsableDebePertenecerAlMismoClub() throws Exception {
        Usuario ajeno = usuario("ajeno", "41000005", RolUsuario.PROFESOR, b, RolClub.PROFESOR);
        String body = payload(TipoGrupo.TALLER, false).replace("}", ",\"profesorUsuarioId\":" + ajeno.getId() + "}");
        mvc.perform(post(base(a) + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test void noPermiteCrearTipoDeshabilitadoNiUsarRutaHistoricaParaEvitarConfiguracion() throws Exception {
        a.setModulos(EnumSet.of(ModuloClub.PARTICIPANTES, ModuloClub.CATEGORIAS));
        clubes.saveAndFlush(a);
        mvc.perform(post(base(a) + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(payload(TipoGrupo.TALLER, false)))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base(a) + "/actividades").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content("""
                {"nombre":"Yoga","profesorUsuarioId":%d,"dias":"Lunes","categoria":"Salud","cupo":32}
                """.formatted(profesor.getId()))).andExpect(status().isBadRequest());
    }

    @Test void noPermiteCupoInvalidoNiReducirloDebajoDeInscripciones() throws Exception {
        String body = payload(TipoGrupo.TALLER, false).replace("}", ",\"cupo\":0}");
        mvc.perform(post(base(a) + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        long id = crear(a, TipoGrupo.TALLER, false);
        inscribir(id);
        Socio segunda = new Socio(null, "Ana", "Perez", "40111223", "ACTIVO");
        segunda.setClub(a); segunda = socios.saveAndFlush(segunda);
        mvc.perform(put(base(a) + "/socios/" + segunda.getId() + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content("{\"grupoIds\":[" + id + "]}")).andExpect(status().isOk());
        mvc.perform(put(base(a) + "/grupos/" + id).header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content(payload(TipoGrupo.TALLER, false).replace("}", ",\"cupo\":1}")))
                .andExpect(status().isBadRequest());
    }

    @Test void inscripcionesNulasNoDanDeBajaVinculosYParticipanteAjenoNoSeConsulta() throws Exception {
        long id = crear(a, TipoGrupo.CATEGORIA_DEPORTIVA, false);
        inscribir(id);
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, inscripciones.countByClubIdAndActividadIdAndEstado(a.getId(), id, EstadoInscripcion.ACTIVA));
        mvc.perform(get(base(b) + "/socios/" + persona.getId() + "/grupos").header("Authorization", "Bearer super"))
                .andExpect(status().isBadRequest());
    }

    private String base(Club club) { return "/api/clubes/" + club.getId(); }
    private String payload(TipoGrupo tipo, boolean asistencia) {
        return "{\"nombre\":\"" + tipo + "\",\"tipo\":\"" + tipo + "\",\"asistenciaHabilitada\":" + asistencia + "}";
    }
    private long crear(Club club, TipoGrupo tipo, boolean asistencia) throws Exception {
        var response = mvc.perform(post(base(club) + "/grupos").header("Authorization", "Bearer super")
                .contentType(APPLICATION_JSON).content(payload(tipo, asistencia)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private void inscribir(long id) throws Exception {
        mvc.perform(put(base(a) + "/socios/" + persona.getId() + "/grupos").header("Authorization", "Bearer admin")
                .contentType(APPLICATION_JSON).content("{\"grupoIds\":[" + id + "]}"))
                .andExpect(status().isOk());
    }
    private Usuario usuario(String token, String dni, RolUsuario rol, Club club, RolClub rolClub) {
        Usuario usuario = usuarios.saveAndFlush(new Usuario(dni, "Nombre", "Apellido", rol, EstadoUsuario.ACTIVO, "hash-prueba", false));
        if (club != null) asignaciones.saveAndFlush(new UsuarioClub(usuario, club, rolClub));
        sesiones.saveAndFlush(new SesionUsuario(tokens.hash(token), LocalDateTime.now().plusHours(1), usuario));
        return usuario;
    }
}
