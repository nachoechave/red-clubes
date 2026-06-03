package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.Club;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class UsuarioClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    private RolClub rol;

    public UsuarioClub() {
    }

    public UsuarioClub(Usuario usuario, Club club, RolClub rol) {
        this.usuario = usuario;
        this.club = club;
        this.rol = rol;
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

    public RolClub getRol() {
        return rol;
    }
}
