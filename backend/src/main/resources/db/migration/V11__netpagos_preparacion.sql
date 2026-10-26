CREATE TABLE net_payment_intent (
    id VARCHAR(36) PRIMARY KEY,
    club_id BIGINT NOT NULL,
    socio_id BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    payer_hash VARCHAR(64) NOT NULL,
    gender VARCHAR(1) NOT NULL,
    provider_uuid VARCHAR(36) NULL UNIQUE,
    checkout_url VARCHAR(2048) NULL,
    provider_mode VARCHAR(255) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    state VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    next_query_at TIMESTAMP(6) NULL,
    issue VARCHAR(80) NULL,
    CONSTRAINT uk_net_request UNIQUE(club_id,socio_id,request_key),
    CONSTRAINT fk_net_intent_club FOREIGN KEY(club_id) REFERENCES club(id),
    CONSTRAINT fk_net_intent_socio FOREIGN KEY(socio_id) REFERENCES socio(id)
);
CREATE INDEX idx_net_intent_soc_date ON net_payment_intent(socio_id,created_at);
CREATE TABLE net_payment_barcode (
    cuota_id BIGINT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    barcode VARCHAR(128) NOT NULL,
    service VARCHAR(80) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    valid_until DATE NOT NULL,
    CONSTRAINT uk_net_barcode UNIQUE(club_id,barcode,service),
    CONSTRAINT fk_net_barcode_cuota FOREIGN KEY(cuota_id) REFERENCES cuota(id),
    CONSTRAINT fk_net_barcode_club FOREIGN KEY(club_id) REFERENCES club(id)
);
CREATE TABLE net_payment_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    intent_id VARCHAR(36) NOT NULL,
    cuota_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    barcode VARCHAR(128) NOT NULL,
    service VARCHAR(80) NOT NULL,
    detail VARCHAR(120) NOT NULL,
    pago_id BIGINT NULL UNIQUE,
    CONSTRAINT uk_net_intent_cuota UNIQUE(intent_id,cuota_id),
    CONSTRAINT fk_net_item_intent FOREIGN KEY(intent_id) REFERENCES net_payment_intent(id),
    CONSTRAINT fk_net_item_cuota FOREIGN KEY(cuota_id) REFERENCES cuota(id),
    CONSTRAINT fk_net_item_pago FOREIGN KEY(pago_id) REFERENCES pago(id)
);
CREATE TABLE net_payment_reservation (
    cuota_id BIGINT PRIMARY KEY,
    intent_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_net_reserve_cuota FOREIGN KEY(cuota_id) REFERENCES cuota(id),
    CONSTRAINT fk_net_reserve_intent FOREIGN KEY(intent_id) REFERENCES net_payment_intent(id)
);
CREATE INDEX idx_net_reserve_intent ON net_payment_reservation(intent_id);
CREATE TABLE net_webhook_event (
    fingerprint VARCHAR(64) PRIMARY KEY,
    intent_id VARCHAR(36) NOT NULL,
    reported_status VARCHAR(30) NOT NULL,
    received_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_net_webhook_intent FOREIGN KEY(intent_id) REFERENCES net_payment_intent(id)
);
CREATE INDEX idx_net_webhook_intent ON net_webhook_event(intent_id);
ALTER TABLE pago ADD COLUMN referencia_externa VARCHAR(100) NULL;
CREATE UNIQUE INDEX uk_pago_referencia_externa ON pago(referencia_externa);
