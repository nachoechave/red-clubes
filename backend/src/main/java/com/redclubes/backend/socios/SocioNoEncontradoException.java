package com.redclubes.backend.socios;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SocioNoEncontradoException extends RuntimeException {

    public SocioNoEncontradoException(Long id) {
        super("Socio no encontrado con id: " + id);
    }
}