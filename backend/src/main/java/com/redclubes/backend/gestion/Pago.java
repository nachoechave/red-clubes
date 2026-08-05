package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Club club;

    @ManyToOne(optional = false)
    private Cuota cuota;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal importe;
    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    private MedioPago medioPago;

    @ManyToOne
    private Usuario usuarioResponsable;

    private String observaciones;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private LocalDateTime fechaAnulacion;

    @ManyToOne
    private Usuario usuarioAnulacion;

    public Long getId() { return id; }
    public Club getClub() { return club; }
    public Cuota getCuota() { return cuota; }
    public BigDecimal getImporte() { return importe; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public MedioPago getMedioPago() { return medioPago; }
    public Usuario getUsuarioResponsable() { return usuarioResponsable; }
    public String getObservaciones() { return observaciones; }
    public EstadoPago getEstado() { return estado; }
    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public Usuario getUsuarioAnulacion() { return usuarioAnulacion; }

    public void setClub(Club club) { this.club = club; }
    public void setCuota(Cuota cuota) { this.cuota = cuota; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    public void setMedioPago(MedioPago medioPago) { this.medioPago = medioPago; }
    public void setUsuarioResponsable(Usuario usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
    public void setUsuarioAnulacion(Usuario usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
}
