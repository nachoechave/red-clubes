package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

@Entity
public class InscripcionActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Club club;

    @ManyToOne
    private Socio socio;

    @ManyToOne
    private Actividad actividad;

    private LocalDate fechaInscripcion;

    private LocalDate fechaBaja;

    private String observaciones;

    @ManyToOne
    private Usuario usuarioResponsable;

    @Enumerated(EnumType.STRING)
    private EstadoInscripcion estado;

    public Long getId() {
        return id;
    }

    public Club getClub() {
        return club;
    }

    public Socio getSocio() {
        return socio;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Usuario getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setUsuarioResponsable(Usuario usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }
}
