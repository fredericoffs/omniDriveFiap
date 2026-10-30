CREATE TABLE sale
(
    id         UUID PRIMARY KEY,
    vehicle_id UUID           NOT NULL REFERENCES vehicle (id),
    buyer_cpf  VARCHAR(11)    NOT NULL CHECK (buyer_cpf ~ '^[0-9]{11}$'),
    sale_date  DATE           NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_sale_vehicle_id ON sale (vehicle_id);

CREATE TABLE payment
(
    id           UUID PRIMARY KEY,
    payment_code VARCHAR(64) NOT NULL,
    sale_id      UUID        NOT NULL REFERENCES sale (id),
    status       VARCHAR(20) NOT NULL CHECK (status IN ('AWAITING_PAYMENT', 'PAID', 'DECLINED', 'CANCELED', 'REFUNDED')),
    version      BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_payment_code UNIQUE (payment_code),
    CONSTRAINT uk_payment_sale UNIQUE (sale_id)
);

ALTER TABLE vehicle
    ADD CONSTRAINT fk_vehicle_current_sale FOREIGN KEY (current_sale_id) REFERENCES sale (id);

ALTER TABLE vehicle_status_history
    ADD CONSTRAINT fk_vehicle_status_history_sale FOREIGN KEY (sale_id) REFERENCES sale (id);
