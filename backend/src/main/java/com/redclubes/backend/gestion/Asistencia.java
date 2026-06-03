package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.Socio;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate fecha;

    private boolean presente;

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
        return presente;
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
