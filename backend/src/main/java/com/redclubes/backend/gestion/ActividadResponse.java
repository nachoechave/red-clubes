package com.redclubes.backend.gestion;

public record ActividadResponse(
        Long id,
        Long clubId,
        String clubNombre,
        String nombre,
        String profesor,
        Long profesorUsuarioId,
        String dias,
        String categoria,
        String icono,
        int cupo,
        int inscriptos,
        EstadoActividad estado
) {
    public static ActividadResponse desde(Actividad actividad, int inscriptosActivos) {
        return new ActividadResponse(
                actividad.getId(),
                actividad.getClub().getId(),
                actividad.getClub().getNombre(),
                actividad.getNombre(),
                actividad.getProfesor(),
                actividad.getProfesorUsuario() == null ? null : actividad.getProfesorUsuario().getId(),
                actividad.getDias(),
                actividad.getCategoria(),
                actividad.getIcono(),
                actividad.getCupo(),
                inscriptosActivos,
                actividad.getEstado()
        );
    }
}
