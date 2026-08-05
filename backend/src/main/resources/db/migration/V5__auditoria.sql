CREATE TABLE auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    club_id BIGINT NULL,
    accion VARCHAR(50) NOT NULL,
    tipo_entidad VARCHAR(80) NOT NULL,
    entidad_id BIGINT NULL,
    fecha TIMESTAMP NOT NULL,
    detalle VARCHAR(1000) NULL,
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_auditoria_club FOREIGN KEY (club_id) REFERENCES club (id)
);

CREATE INDEX idx_auditoria_club_fecha ON auditoria (club_id, fecha);
CREATE INDEX idx_auditoria_usuario_fecha ON auditoria (usuario_id, fecha);
CREATE INDEX idx_auditoria_entidad ON auditoria (tipo_entidad, entidad_id);
