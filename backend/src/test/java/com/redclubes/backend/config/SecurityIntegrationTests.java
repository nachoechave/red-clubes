package com.redclubes.backend.config;

import com.redclubes.backend.usuarios.EstadoUsuario;
import com.redclubes.backend.usuarios.RolUsuario;
import com.redclubes.backend.usuarios.SesionUsuario;
import com.redclubes.backend.usuarios.SesionUsuarioRepository;
import com.redclubes.backend.usuarios.SessionTokenService;
import com.redclubes.backend.usuarios.Usuario;
import com.redclubes.backend.usuarios.UsuarioRepository;
import com.redclubes.backend.usuarios.PasswordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SesionUsuarioRepository sesionUsuarioRepository;
    @Autowired private SessionTokenService sessionTokenService;
    @Autowired private PasswordService passwordService;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void limpiarDatos() {
        sesionUsuarioRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointPrivadoSinSesionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clubes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void sesionValidaPermiteAcceso() throws Exception {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "12345678", "Super", "Prueba", RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO, "hash-no-utilizado", false
        ));
        sesionUsuarioRepository.save(new SesionUsuario(
                sessionTokenService.hash("token-seguro-de-prueba"), LocalDateTime.now().plusHours(1), usuario
        ));

        mockMvc.perform(get("/api/clubes")
                        .header("Authorization", "Bearer token-seguro-de-prueba"))
                .andExpect(status().isOk());
    }

    @Test
    void loginDevuelveBearerPeroSoloPersisteSuHash() throws Exception {
        usuarioRepository.save(new Usuario(
                "87654321", "Ada", "Lovelace", RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO, passwordService.generarHash("contrasena-segura"), false
        ));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dni\":\"87654321\",\"password\":\"contrasena-segura\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bearer = objectMapper.readTree(response).get("token").asText();

        SesionUsuario stored = sesionUsuarioRepository.findByTokenHash(sessionTokenService.hash(bearer)).orElseThrow();
        assertNotEquals(bearer, stored.getTokenHash());
        assertTrue(stored.getTokenHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void profesorAutenticadoRecibe403EnAdministracionDeUsuarios() throws Exception {
        Usuario profesor = usuarioRepository.save(new Usuario(
                "11223344", "Grace", "Hopper", RolUsuario.PROFESOR,
                EstadoUsuario.ACTIVO, "hash-no-utilizado", false
        ));
        sesionUsuarioRepository.save(new SesionUsuario(
                sessionTokenService.hash("token-profesor"), LocalDateTime.now().plusHours(1), profesor
        ));

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void validacionDeLoginDevuelveContratoUniforme() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dni\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dni").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
