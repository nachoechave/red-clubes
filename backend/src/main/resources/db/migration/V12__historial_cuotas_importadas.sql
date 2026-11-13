-- Estado declarado en el archivo de origen, no representa un cobro contable.
CREATE TABLE historial_cuota_importada (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    socio_id BIGINT NOT NULL,
    periodo VARCHAR(7) NOT NULL,
    estado VARCHAR(16) NOT NULL,
    fuente VARCHAR(80) NOT NULL DEFAULT 'CSV',
    registrado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registrado_por BIGINT NULL,
    CONSTRAINT uq_historial_cuota_club_socio_periodo UNIQUE (club_id, socio_id, periodo),
    CONSTRAINT fk_historial_cuota_club FOREIGN KEY (club_id) REFERENCES club(id),
    CONSTRAINT fk_historial_cuota_socio FOREIGN KEY (socio_id) REFERENCES socio(id)
);
