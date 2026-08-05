CREATE TABLE club (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    direccion VARCHAR(120) NOT NULL,
    logo_url ${logo_type} NULL,
    estado VARCHAR(255) NOT NULL,
    CONSTRAINT pk_club PRIMARY KEY (id),
    CONSTRAINT uk_club_nombre UNIQUE (nombre)
);

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dni VARCHAR(10) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    rol VARCHAR(255) NOT NULL,
    estado VARCHAR(255) NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    debe_cambiar_password BOOLEAN NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_dni UNIQUE (dni)
);

CREATE TABLE socio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    dni VARCHAR(10) NOT NULL,
    estado VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    fecha_nacimiento DATE NULL,
    fecha_alta DATE NULL,
    numero_socio INTEGER NULL,
    direccion VARCHAR(255) NULL,
    emergencia_nombre VARCHAR(255) NULL,
    emergencia_telefono VARCHAR(255) NULL,
    emergencia_relacion VARCHAR(255) NULL,
    club_id BIGINT NULL,
    CONSTRAINT pk_socio PRIMARY KEY (id),
    CONSTRAINT fk_socio_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT uk_socio_club_dni UNIQUE (club_id, dni),
    CONSTRAINT uk_socio_club_numero UNIQUE (club_id, numero_socio)
);

CREATE INDEX idx_socio_club_estado ON socio (club_id, estado);
CREATE INDEX idx_socio_club_apellido ON socio (club_id, apellido);

CREATE TABLE actividad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    profesor VARCHAR(255) NOT NULL,
    dias VARCHAR(255) NOT NULL,
    categoria VARCHAR(255) NOT NULL,
    icono VARCHAR(255) NOT NULL,
    cupo INTEGER NOT NULL,
    inscriptos INTEGER NOT NULL,
    estado VARCHAR(255) NOT NULL,
    club_id BIGINT NULL,
    CONSTRAINT pk_actividad PRIMARY KEY (id),
    CONSTRAINT fk_actividad_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT uk_actividad_club_nombre UNIQUE (club_id, nombre)
);

CREATE INDEX idx_actividad_club_estado ON actividad (club_id, estado);

CREATE TABLE cuota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mes VARCHAR(255) NOT NULL,
    importe INTEGER NOT NULL,
    estado VARCHAR(255) NOT NULL,
    vencimiento DATE NOT NULL,
    club_id BIGINT NULL,
    socio_id BIGINT NULL,
    CONSTRAINT pk_cuota PRIMARY KEY (id),
    CONSTRAINT fk_cuota_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT fk_cuota_socio FOREIGN KEY (socio_id) REFERENCES socio (id),
    CONSTRAINT uk_cuota_club_socio_mes UNIQUE (club_id, socio_id, mes)
);

CREATE INDEX idx_cuota_club_estado ON cuota (club_id, estado);
CREATE INDEX idx_cuota_club_vencimiento ON cuota (club_id, vencimiento);

CREATE TABLE inscripcion_actividad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NULL,
    socio_id BIGINT NULL,
    actividad_id BIGINT NULL,
    fecha_inscripcion DATE NULL,
    estado VARCHAR(255) NULL,
    CONSTRAINT pk_inscripcion_actividad PRIMARY KEY (id),
    CONSTRAINT fk_inscripcion_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT fk_inscripcion_socio FOREIGN KEY (socio_id) REFERENCES socio (id),
    CONSTRAINT fk_inscripcion_actividad FOREIGN KEY (actividad_id) REFERENCES actividad (id),
    CONSTRAINT uk_inscripcion_club_socio_actividad UNIQUE (club_id, socio_id, actividad_id)
);

CREATE INDEX idx_inscripcion_actividad_estado ON inscripcion_actividad (club_id, actividad_id, estado);
CREATE INDEX idx_inscripcion_socio_estado ON inscripcion_actividad (club_id, socio_id, estado);

CREATE TABLE asistencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATE NOT NULL,
    presente BOOLEAN NOT NULL,
    club_id BIGINT NULL,
    actividad_id BIGINT NULL,
    socio_id BIGINT NULL,
    CONSTRAINT pk_asistencia PRIMARY KEY (id),
    CONSTRAINT fk_asistencia_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT fk_asistencia_actividad FOREIGN KEY (actividad_id) REFERENCES actividad (id),
    CONSTRAINT fk_asistencia_socio FOREIGN KEY (socio_id) REFERENCES socio (id),
    CONSTRAINT uk_asistencia_club_actividad_socio_fecha UNIQUE (club_id, actividad_id, socio_id, fecha)
);

CREATE INDEX idx_asistencia_club_fecha ON asistencia (club_id, fecha);
CREATE INDEX idx_asistencia_actividad_fecha ON asistencia (club_id, actividad_id, fecha);

CREATE TABLE usuario_club (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    rol VARCHAR(255) NOT NULL,
    CONSTRAINT pk_usuario_club PRIMARY KEY (id),
    CONSTRAINT fk_usuario_club_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_club_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT uk_usuario_club UNIQUE (usuario_id, club_id)
);

CREATE INDEX idx_usuario_club_club_rol ON usuario_club (club_id, rol);

CREATE TABLE usuario_actividad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL,
    CONSTRAINT pk_usuario_actividad PRIMARY KEY (id),
    CONSTRAINT fk_usuario_actividad_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_actividad_club FOREIGN KEY (club_id) REFERENCES club (id),
    CONSTRAINT fk_usuario_actividad_actividad FOREIGN KEY (actividad_id) REFERENCES actividad (id),
    CONSTRAINT uk_usuario_actividad UNIQUE (usuario_id, club_id, actividad_id)
);

CREATE INDEX idx_usuario_actividad_club ON usuario_actividad (club_id, actividad_id);

CREATE TABLE sesion_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(255) NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT pk_sesion_usuario PRIMARY KEY (id),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT uk_sesion_usuario_token UNIQUE (token)
);

CREATE INDEX idx_sesion_usuario_expiracion ON sesion_usuario (fecha_expiracion);
