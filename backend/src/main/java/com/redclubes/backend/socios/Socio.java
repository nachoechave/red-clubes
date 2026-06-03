package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.Club;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 7, max = 10, message = "El DNI debe tener entre 7 y 10 caracteres")
    private String dni;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    private String telefono;

    private String direccion;

    private String emergenciaNombre;

    private String emergenciaTelefono;

    private String emergenciaRelacion;

    @ManyToOne
    private Club club;

    public Socio() {
    }

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

    public Club getClub() {
        return club;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEmergenciaNombre() {
        return emergenciaNombre;
    }

    public String getEmergenciaTelefono() {
        return emergenciaTelefono;
    }

    public String getEmergenciaRelacion() {
        return emergenciaRelacion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setEmergenciaNombre(String emergenciaNombre) {
        this.emergenciaNombre = emergenciaNombre;
    }

    public void setEmergenciaTelefono(String emergenciaTelefono) {
        this.emergenciaTelefono = emergenciaTelefono;
    }

    public void setEmergenciaRelacion(String emergenciaRelacion) {
        this.emergenciaRelacion = emergenciaRelacion;
    }
}
