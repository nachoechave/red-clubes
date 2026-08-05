package com.redclubes.backend.usuarios;

public class AutenticacionRequeridaException extends RuntimeException {

    public AutenticacionRequeridaException() {
        super("Se requiere una sesion valida");
    }
}
