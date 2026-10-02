CREATE TABLE vehicle
(
    id             UUID PRIMARY KEY,
    brand          VARCHAR(60)    NOT NULL,
    model          VARCHAR(60)    NOT NULL,
    year           SMALLINT       NOT NULL,
    color          VARCHAR(30)    NOT NULL,
    purchase_price NUMERIC(12, 2) NOT NULL CHECK (purchase_price > 0),
    sale_price     NUMERIC(12, 2) NOT NULL CHECK (sale_price > 0),
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD')),
    origin         VARCHAR(20)    NOT NULL CHECK (origin IN ('OWNED', 'CONSIGNED')),
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_vehicle_status_sale_price ON vehicle (status, sale_price);

CREATE TABLE vehicle_status_history
(
    id              UUID PRIMARY KEY,
    vehicle_id      UUID        NOT NULL REFERENCES vehicle (id),
    previous_status VARCHAR(20) CHECK (previous_status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD')),
    new_status      VARCHAR(20) NOT NULL CHECK (new_status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD')),
    reason          VARCHAR(255),
    sale_id         UUID,
    occurred_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_vehicle_status_history_vehicle_id ON vehicle_status_history (vehicle_id, occurred_at);
