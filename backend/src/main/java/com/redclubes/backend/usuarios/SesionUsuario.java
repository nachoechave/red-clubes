package com.redclubes.backend.usuarios;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime fechaExpiracion;

    @ManyToOne(optional = false)
    private Usuario usuario;

    public SesionUsuario() {
    }

    public SesionUsuario(String token, LocalDateTime fechaExpiracion, Usuario usuario) {
        this.token = token;
        this.fechaExpiracion = fechaExpiracion;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
