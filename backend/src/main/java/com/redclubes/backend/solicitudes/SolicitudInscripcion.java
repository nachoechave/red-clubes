package com.redclubes.backend.solicitudes;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SolicitudInscripcion {
    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 36) String referencia;
    @Column(nullable = false) Long clubId;
    @Column(nullable = false, length = 36) String claveEnvio;
    @Column(nullable = false, length = 64) String huella;
    @Column(nullable = false, length = 50) String nombre;
    @Column(nullable = false, length = 50) String apellido;
    @Column(nullable = false, length = 10) String dni;
    @Column(length = 10) String dniPendiente;
    @Column(nullable = false, length = 120) String email;
    @Column(nullable = false, length = 30) String telefono;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Estado estado;
    @Column(nullable = false, length = 30) String consentimientoVersion;
    @Column(nullable = false) LocalDateTime fechaCreacion;
    LocalDateTime fechaRevision;
    Long usuarioRevisionId;
    Long socioId;
    @Column(length = 500) String motivo;
    protected SolicitudInscripcion() {}
    void actualizar(DatosSolicitud datos) {
        nombre = datos.nombre(); apellido = datos.apellido(); dni = datos.dni();
        email = datos.email(); telefono = datos.telefono();
    }
}
