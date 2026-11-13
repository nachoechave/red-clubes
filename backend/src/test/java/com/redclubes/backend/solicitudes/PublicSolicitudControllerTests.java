package com.redclubes.backend.solicitudes;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicSolicitudControllerTests {
    @Test void limitaPorIpYNoUsaXForwardedForSinNormalizar() {
        var servicio = mock(SolicitudService.class);
        var controller = new PublicSolicitudController(servicio,2);
        var http = new MockHttpServletRequest(); http.setRemoteAddr("192.0.2.1");
        var request = new CrearSolicitudRequest(new DatosSolicitud("Ana","Prueba","99111222","test@example.invalid","2215551234"),true,"2026-10-v1",UUID.randomUUID().toString(),"");
        controller.recibir("bomberos-ensenada",request,http);
        http.addHeader("X-Forwarded-For","192.0.2.2");
        controller.recibir("bomberos-ensenada",request,http);
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> controller.recibir("bomberos-ensenada",request,http)).getStatusCode().value());
        verify(servicio,times(2)).recibir("bomberos-ensenada",request);
        assertFalse(PublicSolicitudController.esRutaPublica("GET","/api/public/instituciones/bomberos-ensenada/solicitudes"));
        assertFalse(PublicSolicitudController.esRutaPublica("POST","/api/clubes/1/socios"));
    }
}
