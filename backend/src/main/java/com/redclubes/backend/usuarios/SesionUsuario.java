package com.redclubes.backend.usuarios;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

@Entity
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String tokenHash;

    private LocalDateTime fechaExpiracion;

    @ManyToOne(optional = false)
    private Usuario usuario;

    public SesionUsuario() {
    }

    public SesionUsuario(String tokenHash, LocalDateTime fechaExpiracion, Usuario usuario) {
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
