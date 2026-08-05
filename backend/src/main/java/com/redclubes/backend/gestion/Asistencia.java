package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    private boolean presente;

    @Enumerated(EnumType.STRING)
    private EstadoAsistencia estado;

    private LocalDateTime fechaActualizacion;

    @ManyToOne
    private Usuario usuarioResponsable;

    @ManyToOne
    private Club club;

    @ManyToOne
    private Actividad actividad;

    @ManyToOne
    private Socio socio;

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public boolean isPresente() {
        return estado == null ? presente : estado == EstadoAsistencia.PRESENTE;
    }

    public EstadoAsistencia getEstado() {
        return estado == null ? (presente ? EstadoAsistencia.PRESENTE : EstadoAsistencia.AUSENTE) : estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public Usuario getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public Club getClub() {
        return club;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
        this.estado = presente ? EstadoAsistencia.PRESENTE : EstadoAsistencia.AUSENTE;
    }

    public void setEstado(EstadoAsistencia estado) {
        this.estado = estado;
        this.presente = estado == EstadoAsistencia.PRESENTE;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public void setUsuarioResponsable(Usuario usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }
}
