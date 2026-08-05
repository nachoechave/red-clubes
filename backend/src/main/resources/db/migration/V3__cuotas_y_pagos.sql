ALTER TABLE cuota ADD COLUMN periodo VARCHAR(7) NULL;
ALTER TABLE cuota ADD COLUMN importe_decimal DECIMAL(12,2) NULL;
ALTER TABLE cuota ADD COLUMN fecha_emision DATE NULL;

UPDATE cuota SET periodo = CASE
    WHEN mes LIKE '____-__' THEN mes
    WHEN LOWER(mes) LIKE 'enero %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-01')
    WHEN LOWER(mes) LIKE 'febrero %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-02')
    WHEN LOWER(mes) LIKE 'marzo %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-03')
    WHEN LOWER(mes) LIKE 'abril %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-04')
    WHEN LOWER(mes) LIKE 'mayo %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-05')
    WHEN LOWER(mes) LIKE 'junio %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-06')
    WHEN LOWER(mes) LIKE 'julio %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-07')
    WHEN LOWER(mes) LIKE 'agosto %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-08')
    WHEN LOWER(mes) LIKE 'septiembre %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-09')
    WHEN LOWER(mes) LIKE 'octubre %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-10')
    WHEN LOWER(mes) LIKE 'noviembre %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-11')
    WHEN LOWER(mes) LIKE 'diciembre %' THEN CONCAT(RIGHT(TRIM(mes), 4), '-12')
    ELSE NULL
END WHERE periodo IS NULL;

UPDATE cuota SET importe_decimal = importe WHERE importe_decimal IS NULL;
UPDATE cuota SET estado = 'PAGADA' WHERE estado = 'PAGADO';

CREATE UNIQUE INDEX uk_cuota_club_socio_periodo ON cuota (club_id, socio_id, periodo);
CREATE INDEX idx_cuota_club_periodo_estado ON cuota (club_id, periodo, estado);

CREATE TABLE pago (
    id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    cuota_id BIGINT NOT NULL,
    importe DECIMAL(12,2) NOT NULL,
    fecha_pago TIMESTAMP NULL,
    medio_pago VARCHAR(30) NOT NULL,
    usuario_responsable_id BIGINT NULL,
    observaciones VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_anulacion TIMESTAMP NULL,
    usuario_anulacion_id BIGINT NULL,
    CONSTRAINT pk_pago PRIMARY KEY (id),
    CONSTRAINT fk_pago_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT fk_pago_cuota FOREIGN KEY (cuota_id) REFERENCES cuota (id),
    CONSTRAINT fk_pago_usuario_responsable FOREIGN KEY (usuario_responsable_id) REFERENCES usuario (id),
    CONSTRAINT fk_pago_usuario_anulacion FOREIGN KEY (usuario_anulacion_id) REFERENCES usuario (id)
);

CREATE INDEX idx_pago_club_fecha ON pago (club_id, fecha_pago);
CREATE INDEX idx_pago_cuota_estado ON pago (club_id, cuota_id, estado);

INSERT INTO pago (club_id, cuota_id, importe, fecha_pago, medio_pago, observaciones, estado)
SELECT club_id, id, importe_decimal, NULL, 'MIGRACION', 'Pago historico migrado; fecha original no disponible', 'ACTIVO'
FROM cuota
WHERE estado = 'PAGADA';
