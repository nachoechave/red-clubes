package com.redclubes.backend.usuarios;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_CLAVE = 256;
    private static final int LONGITUD_SALT = 16;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generarHash(String password) {
        byte[] salt = new byte[LONGITUD_SALT];
        secureRandom.nextBytes(salt);
        byte[] hash = calcularHash(password, salt);

        return ITERACIONES
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean coincide(String password, String passwordHash) {
        String[] partes = passwordHash.split(":");
        if (partes.length != 3) {
            return false;
        }

        int iteraciones = Integer.parseInt(partes[0]);
        byte[] salt = Base64.getDecoder().decode(partes[1]);
        byte[] hashGuardado = Base64.getDecoder().decode(partes[2]);
        byte[] hashIngresado = calcularHash(password, salt, iteraciones);

        if (hashGuardado.length != hashIngresado.length) {
            return false;
        }

        int diferencias = 0;
        for (int i = 0; i < hashGuardado.length; i++) {
            diferencias |= hashGuardado[i] ^ hashIngresado[i];
        }

        return diferencias == 0;
    }

    private byte[] calcularHash(String password, byte[] salt) {
        return calcularHash(password, salt, ITERACIONES);
    }

    private byte[] calcularHash(String password, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteraciones, LONGITUD_CLAVE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el hash de la contrasena", exception);
        }
    }
}
