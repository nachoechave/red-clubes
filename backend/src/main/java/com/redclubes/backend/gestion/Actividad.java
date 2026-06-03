package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String profesor;

    @NotBlank
    private String dias;

    @NotBlank
    private String categoria;

    @NotBlank
    private String icono;

    @Min(1)
    private int cupo;

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
        return profesor;
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

    public int getCupo() {
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

    public void setDias(String dias) {
        this.dias = dias;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public void setCupo(int cupo) {
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
