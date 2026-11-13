package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Actividad {

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoGrupo tipo = TipoGrupo.ACTIVIDAD;
    private String disciplina;
    private String anioCategoria;
    private boolean asistenciaHabilitada = true;

    public TipoGrupo getTipo() { return tipo; }
    public void setTipo(TipoGrupo tipo) { this.tipo = tipo; }
    public String getDisciplina() { return disciplina; }
    public void setDisciplina(String disciplina) { this.disciplina = disciplina; }
    public String getAnioCategoria() { return anioCategoria; }
    public void setAnioCategoria(String anioCategoria) { this.anioCategoria = anioCategoria; }
    public boolean isAsistenciaHabilitada() { return asistenciaHabilitada; }
    public void setAsistenciaHabilitada(boolean habilitada) { this.asistenciaHabilitada = habilitada; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    private String profesor;

    @ManyToOne
    private Usuario profesorUsuario;

    private String dias;

    private String categoria;

    private String icono;

    @Min(1)
    private Integer cupo;

    @Min(0)
    private int inscriptos;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoActividad estado;

    @ManyToOne
    private Club club;

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getProfesor() {
        return profesorUsuario == null
                ? profesor
                : (profesorUsuario.getNombre() + " " + profesorUsuario.getApellido()).trim();
    }

    public Usuario getProfesorUsuario() {
        return profesorUsuario;
    }

    public String getDias() {
        return dias;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getIcono() {
        return icono;
    }

    public Integer getCupo() {
        return cupo;
    }

    public int getInscriptos() {
        return inscriptos;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public Club getClub() {
        return club;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setProfesor(String profesor) {
        this.profesor = profesor;
    }

    public void setProfesorUsuario(Usuario profesorUsuario) {
        this.profesorUsuario = profesorUsuario;
        if (profesorUsuario != null) {
            this.profesor = (profesorUsuario.getNombre() + " " + profesorUsuario.getApellido()).trim();
        }
    }

    public void setDias(String dias) {
        this.dias = dias;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public void setInscriptos(int inscriptos) {
        this.inscriptos = inscriptos;
    }

    public void setEstado(EstadoActividad estado) {
        this.estado = estado;
    }

    public void setClub(Club club) {
        this.club = club;
    }
}
