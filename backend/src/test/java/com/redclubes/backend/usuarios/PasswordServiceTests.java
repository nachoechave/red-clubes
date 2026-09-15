package com.redclubes.backend.usuarios;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTests {
    private final PasswordService passwordService = new PasswordService();

    @Test
    void generaHashesConElCostoActual() {
        String hash = passwordService.generarHash("contrasena-segura");

        assertTrue(hash.startsWith(PasswordService.ITERACIONES_ACTUALES + ":"));
        assertTrue(passwordService.coincide("contrasena-segura", hash));
        assertFalse(passwordService.necesitaRehash(hash));
    }

    @Test
    void conservaCompatibilidadConHashesAnteriores() throws Exception {
        String hashAnterior = generarHash("contrasena-segura", 120_000);

        assertTrue(passwordService.coincide("contrasena-segura", hashAnterior));
        assertTrue(passwordService.necesitaRehash(hashAnterior));
    }

    @Test
    void rechazaPasswordIncorrectoYFormatosInvalidos() {
        String hash = passwordService.generarHash("contrasena-segura");

        assertFalse(passwordService.coincide("incorrecta", hash));
        assertFalse(passwordService.coincide("contrasena-segura", "formato-invalido"));
        assertFalse(passwordService.coincide("contrasena-segura", "999999999:AAAA:AAAA"));
    }

    private String generarHash(String password, int iteraciones) throws Exception {
        byte[] salt = new byte[16];
        for (int indice = 0; indice < salt.length; indice++) {
            salt[indice] = (byte) (indice + 1);
        }
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteraciones, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return iteraciones + ":" + Base64.getEncoder().encodeToString(salt)
                    + ":" + Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }
}
