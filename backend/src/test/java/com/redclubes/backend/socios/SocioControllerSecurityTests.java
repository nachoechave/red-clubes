package com.redclubes.backend.socios;

import com.redclubes.backend.config.GlobalExceptionHandler;
import com.redclubes.backend.config.SecurityErrorWriter;
import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.AutenticacionRequeridaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SocioController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SocioControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocioService socioService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityErrorWriter securityErrorWriter;

    @Test
    void usuarioSinSesionNoPuedeConsultarSocios() throws Exception {
        doThrow(new AutenticacionRequeridaException())
                .when(authService).exigirOperadorDeClub(null, 1L);

        mockMvc.perform(get("/api/clubes/1/socios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }
}
