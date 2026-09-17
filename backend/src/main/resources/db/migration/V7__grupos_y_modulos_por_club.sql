ALTER TABLE actividad ADD COLUMN tipo VARCHAR(255) NOT NULL DEFAULT 'ACTIVIDAD';
ALTER TABLE actividad ADD COLUMN disciplina VARCHAR(255) NULL;
ALTER TABLE actividad ADD COLUMN anio_categoria VARCHAR(255) NULL;
ALTER TABLE actividad ADD COLUMN asistencia_habilitada BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE actividad MODIFY COLUMN profesor VARCHAR(255) NULL;
ALTER TABLE actividad MODIFY COLUMN dias VARCHAR(255) NULL;
ALTER TABLE actividad MODIFY COLUMN categoria VARCHAR(255) NULL;
ALTER TABLE actividad MODIFY COLUMN icono VARCHAR(255) NULL;
ALTER TABLE actividad MODIFY COLUMN cupo INTEGER NULL;
CREATE INDEX idx_actividad_club_tipo ON actividad (club_id, tipo);

ALTER TABLE club ADD COLUMN nombre_participantes VARCHAR(255) NOT NULL DEFAULT 'SOCIOS';
CREATE TABLE club_modulo (
    club_id BIGINT NOT NULL,
    modulo VARCHAR(255) NOT NULL,
    PRIMARY KEY (club_id, modulo),
    CONSTRAINT fk_club_modulo_club FOREIGN KEY (club_id) REFERENCES club(id)
);
INSERT INTO club_modulo (club_id, modulo) SELECT id, 'PARTICIPANTES' FROM club;
INSERT INTO club_modulo (club_id, modulo) SELECT id, 'ACTIVIDADES' FROM club;
INSERT INTO club_modulo (club_id, modulo) SELECT id, 'PROFESORES' FROM club;
INSERT INTO club_modulo (club_id, modulo) SELECT id, 'ASISTENCIAS' FROM club;
INSERT INTO club_modulo (club_id, modulo) SELECT id, 'CUOTAS' FROM club;
