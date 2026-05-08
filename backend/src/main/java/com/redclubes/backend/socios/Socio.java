package com.redclubes.backend.socios;

public class Socio {

    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String estado;

    public Socio(Long id, String nombre, String apellido, String dni, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDni() {
        return dni;
    }

    public String getEstado() {
        return estado;
    }
}