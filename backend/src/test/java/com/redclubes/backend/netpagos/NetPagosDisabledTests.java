package com.redclubes.backend.netpagos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class NetPagosDisabledTests {
    @Autowired MockMvc mvc;
    @Autowired NetPagosConfig config;
    @Test void webhookIsClosedByDefault() throws Exception {
        assertEquals(NetPagosConfig.Mode.OFF, config.mode);
        mvc.perform(post("/api/integraciones/netpagos/webhook").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
    @Test void portalPaymentsRequirePortalAuthentication() throws Exception {
        mvc.perform(post("/api/portal-bomberos/pagos").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
