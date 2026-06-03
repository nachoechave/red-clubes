package com.redclubes.backend.usuarios;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("DNI o contrasena incorrectos");
    }
}
