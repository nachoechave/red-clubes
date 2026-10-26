-- No existing email is implicitly verified. All grants require assisted verification.
CREATE TABLE portal_verified_email (
    socio_id BIGINT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    revision VARCHAR(36) NOT NULL,
    verified_at TIMESTAMP(6) NOT NULL,
    verified_by BIGINT NOT NULL,
    evidence VARCHAR(255) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_portal_email_socio FOREIGN KEY (socio_id) REFERENCES socio(id),
    CONSTRAINT fk_portal_email_club FOREIGN KEY (club_id) REFERENCES club(id),
    CONSTRAINT fk_portal_email_actor FOREIGN KEY (verified_by) REFERENCES usuario(id)
);
CREATE TABLE portal_challenge (
    id VARCHAR(36) PRIMARY KEY,
    socio_id BIGINT NULL,
    club_id BIGINT NOT NULL,
    revision VARCHAR(36),
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_portal_challenge_socio FOREIGN KEY (socio_id) REFERENCES socio(id)
);
CREATE INDEX idx_portal_challenge_expiry ON portal_challenge(expires_at);
CREATE INDEX idx_portal_challenge_socio ON portal_challenge(socio_id);
CREATE TABLE portal_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    socio_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    revision VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_portal_session_socio FOREIGN KEY (socio_id) REFERENCES socio(id),
    CONSTRAINT fk_portal_session_club FOREIGN KEY (club_id) REFERENCES club(id)
);
CREATE INDEX idx_portal_session_expiry ON portal_session(expires_at);
