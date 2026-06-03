package com.redclubes.backend.clubes;

public class ClubNoEncontradoException extends RuntimeException {

    public ClubNoEncontradoException(Long id) {
        super("Club no encontrado con id: " + id);
    }
}
