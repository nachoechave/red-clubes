package com.redclubes.backend.gestion;

public record ActividadResponse(
        Long id,
        Long clubId,
        String clubNombre,
        String nombre,
        String profesor,
        String dias,
        String categoria,
        String icono,
        int cupo,
        int inscriptos,
        EstadoActividad estado
) {
    public static ActividadResponse desde(Actividad actividad) {
        return new ActividadResponse(
                actividad.getId(),
                actividad.getClub().getId(),
                actividad.getClub().getNombre(),
                actividad.getNombre(),
                actividad.getProfesor(),
                actividad.getDias(),
                actividad.getCategoria(),
                actividad.getIcono(),
                actividad.getCupo(),
                actividad.getInscriptos(),
                actividad.getEstado()
        );
    }
}
