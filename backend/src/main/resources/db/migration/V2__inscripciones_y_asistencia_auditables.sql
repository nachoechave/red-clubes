ALTER TABLE inscripcion_actividad ADD COLUMN fecha_baja DATE NULL;
ALTER TABLE inscripcion_actividad ADD COLUMN observaciones VARCHAR(500) NULL;
ALTER TABLE inscripcion_actividad ADD COLUMN usuario_responsable_id BIGINT NULL;
ALTER TABLE inscripcion_actividad ADD CONSTRAINT fk_inscripcion_usuario_responsable
    FOREIGN KEY (usuario_responsable_id) REFERENCES usuario (id);

ALTER TABLE asistencia ADD COLUMN estado VARCHAR(20) NULL;
ALTER TABLE asistencia ADD COLUMN fecha_actualizacion TIMESTAMP NULL;
ALTER TABLE asistencia ADD COLUMN usuario_responsable_id BIGINT NULL;
ALTER TABLE asistencia ADD CONSTRAINT fk_asistencia_usuario_responsable
    FOREIGN KEY (usuario_responsable_id) REFERENCES usuario (id);

UPDATE asistencia
SET estado = CASE WHEN presente = TRUE THEN 'PRESENTE' ELSE 'AUSENTE' END
WHERE estado IS NULL;

CREATE INDEX idx_inscripcion_usuario_responsable ON inscripcion_actividad (usuario_responsable_id);
CREATE INDEX idx_asistencia_usuario_responsable ON asistencia (usuario_responsable_id);
