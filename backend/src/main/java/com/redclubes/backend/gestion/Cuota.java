package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.socios.Socio;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String mes;

    @Min(0)
    private int importe;

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

    public int getImporte() {
        return importe;
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

    public void setImporte(int importe) {
        this.importe = importe;
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
