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
        Integer cupo,
        int inscriptos,
        EstadoActividad estado,
        TipoGrupo tipo,
        String disciplina,
        String anioCategoria,
        boolean asistenciaHabilitada
) {
    public static ActividadResponse desde(Actividad actividad, int inscriptosActivos) {
        return new ActividadResponse(
                actividad.getId(),
                actividad.getClub().getId(),
                actividad.getClub().getNombre(),
                actividad.getNombre(),
                actividad.getProfesor() == null ? "" : actividad.getProfesor(),
                actividad.getProfesorUsuario() == null ? null : actividad.getProfesorUsuario().getId(),
                actividad.getDias() == null ? "" : actividad.getDias(),
                actividad.getCategoria() == null ? "" : actividad.getCategoria(),
                actividad.getIcono() == null ? "check" : actividad.getIcono(),
                actividad.getCupo(),
                inscriptosActivos,
                actividad.getEstado(),
                actividad.getTipo(),
                actividad.getDisciplina(),
                actividad.getAnioCategoria(),
                actividad.isAsistenciaHabilitada()
        );
    }
}
