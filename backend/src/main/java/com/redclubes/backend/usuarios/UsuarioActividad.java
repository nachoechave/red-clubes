package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.gestion.Actividad;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class UsuarioActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Club club;

    @ManyToOne(optional = false)
    private Actividad actividad;

    public UsuarioActividad() {
    }

    public UsuarioActividad(Usuario usuario, Club club, Actividad actividad) {
        this.usuario = usuario;
        this.club = club;
        this.actividad = actividad;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Club getClub() {
        return club;
    }

    public Actividad getActividad() {
        return actividad;
    }
}
