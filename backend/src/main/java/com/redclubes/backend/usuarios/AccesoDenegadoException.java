package com.redclubes.backend.usuarios;

public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tenes permisos para realizar esta accion");
    }
}
