package com.redclubes.backend.usuarios;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    static final int ITERACIONES_ACTUALES = 600_000;
    private static final int ITERACIONES_MAXIMAS_ACEPTADAS = 10_000_000;
    private static final int LONGITUD_CLAVE = 256;
    private static final int LONGITUD_SALT = 16;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generarHash(String password) {
        byte[] salt = new byte[LONGITUD_SALT];
        secureRandom.nextBytes(salt);
        byte[] hash = calcularHash(password, salt);

        return ITERACIONES_ACTUALES
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean coincide(String password, String passwordHash) {
        HashAlmacenado almacenado = parsear(passwordHash);
        if (almacenado == null) {
            return false;
        }
        byte[] hashIngresado = calcularHash(password, almacenado.salt(), almacenado.iteraciones());
        return MessageDigest.isEqual(almacenado.hash(), hashIngresado);
    }

    public boolean necesitaRehash(String passwordHash) {
        HashAlmacenado almacenado = parsear(passwordHash);
        return almacenado == null || almacenado.iteraciones() < ITERACIONES_ACTUALES;
    }

    private byte[] calcularHash(String password, byte[] salt) {
        return calcularHash(password, salt, ITERACIONES_ACTUALES);
    }

    private byte[] calcularHash(String password, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteraciones, LONGITUD_CLAVE);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
                return factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el hash de la contrasena", exception);
        }
    }

    private HashAlmacenado parsear(String passwordHash) {
        if (passwordHash == null) {
            return null;
        }
        String[] partes = passwordHash.split(":", -1);
        if (partes.length != 3) {
            return null;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            if (iteraciones < 1 || iteraciones > ITERACIONES_MAXIMAS_ACEPTADAS) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] hash = Base64.getDecoder().decode(partes[2]);
            if (salt.length < LONGITUD_SALT || hash.length != LONGITUD_CLAVE / Byte.SIZE) {
                return null;
            }
            return new HashAlmacenado(iteraciones, salt, hash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record HashAlmacenado(int iteraciones, byte[] salt, byte[] hash) {
    }
}
