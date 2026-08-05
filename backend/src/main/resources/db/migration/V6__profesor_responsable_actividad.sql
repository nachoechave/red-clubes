ALTER TABLE actividad ADD COLUMN profesor_usuario_id BIGINT NULL;

ALTER TABLE actividad ADD CONSTRAINT fk_actividad_profesor_usuario
    FOREIGN KEY (profesor_usuario_id) REFERENCES usuario (id);

CREATE INDEX idx_actividad_club_profesor ON actividad (club_id, profesor_usuario_id);
