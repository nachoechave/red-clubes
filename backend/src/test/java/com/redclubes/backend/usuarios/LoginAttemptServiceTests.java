package com.redclubes.backend.usuarios;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTests {
    private final SessionTokenService tokenService = new SessionTokenService();

    @Test
    void bloqueaLaCombinacionDeIdentidadEIpAlAlcanzarElLimite() {
        LoginAttemptService service = new LoginAttemptService(3, 15, 15, tokenService);
        String key = service.key("12345678", "127.0.0.1");

        service.recordFailure(key);
        service.recordFailure(key);
        assertDoesNotThrow(() -> service.verifyAllowed(key));
        service.recordFailure(key);

        assertThrows(CredencialesInvalidasException.class, () -> service.verifyAllowed(key));
    }

    @Test
    void noConservaDniNiIpEnLaClaveYUnExitoLimpiaLosIntentos() {
        LoginAttemptService service = new LoginAttemptService(1, 15, 15, tokenService);
        String key = service.key("12345678", "192.0.2.10");
        assertNotEquals("12345678|192.0.2.10", key);
        service.recordFailure(key);
        service.recordSuccess(key);

        assertDoesNotThrow(() -> service.verifyAllowed(key));
    }
}
