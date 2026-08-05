package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.Socio;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String mes;

    private String periodo;

    private int importe;

    @Column(precision = 12, scale = 2)
    private BigDecimal importeDecimal;

    private LocalDate fechaEmision;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoCuota estado;

    @NotNull
    private LocalDate vencimiento;

    @ManyToOne
    private Club club;

    @ManyToOne
    private Socio socio;

    public Long getId() {
        return id;
    }

    public String getMes() {
        return mes;
    }

    public String getPeriodo() {
        return periodo == null ? mes : periodo;
    }

    public BigDecimal getImporte() {
        return importeDecimal == null ? BigDecimal.valueOf(importe) : importeDecimal;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public EstadoCuota getEstado() {
        return estado;
    }

    public LocalDate getVencimiento() {
        return vencimiento;
    }

    public Club getClub() {
        return club;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
        this.mes = periodo;
    }

    public void setImporte(BigDecimal importe) {
        this.importeDecimal = importe;
        this.importe = importe.intValue();
    }

    public void setImporte(int importe) {
        setImporte(BigDecimal.valueOf(importe));
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public void setEstado(EstadoCuota estado) {
        this.estado = estado;
    }

    public void setVencimiento(LocalDate vencimiento) {
        this.vencimiento = vencimiento;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }
}
