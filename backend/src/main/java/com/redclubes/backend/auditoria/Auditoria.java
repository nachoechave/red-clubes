package com.redclubes.backend.auditoria;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne
    private Club club;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 80)
    private String tipoEntidad;

    private Long entidadId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 1000)
    private String detalle;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Club getClub() { return club; }
    public String getAccion() { return accion; }
    public String getTipoEntidad() { return tipoEntidad; }
    public Long getEntidadId() { return entidadId; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDetalle() { return detalle; }

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setClub(Club club) { this.club = club; }
    public void setAccion(String accion) { this.accion = accion; }
    public void setTipoEntidad(String tipoEntidad) { this.tipoEntidad = tipoEntidad; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
