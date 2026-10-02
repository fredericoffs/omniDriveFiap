CREATE TABLE veiculo (
    id             UUID           PRIMARY KEY,
    marca          VARCHAR(60)    NOT NULL,
    modelo         VARCHAR(60)    NOT NULL,
    ano            SMALLINT       NOT NULL,
    cor            VARCHAR(30)    NOT NULL,
    valor_entrada  NUMERIC(12, 2) NOT NULL CHECK (valor_entrada > 0),
    preco_venda    NUMERIC(12, 2) NOT NULL CHECK (preco_venda > 0),
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('RECEBIDO', 'EM_PREPARACAO', 'DISPONIVEL', 'RESERVADO', 'VENDIDO')),
    origem         VARCHAR(20)    NOT NULL CHECK (origem IN ('PROPRIO', 'CONSIGNADO')),
    criado_em      TIMESTAMPTZ    NOT NULL,
    atualizado_em  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_veiculo_status_preco_venda ON veiculo (status, preco_venda);

CREATE TABLE historico_status_veiculo (
    id               UUID         PRIMARY KEY,
    veiculo_id       UUID         NOT NULL REFERENCES veiculo (id),
    status_anterior  VARCHAR(20)  CHECK (status_anterior IN ('RECEBIDO', 'EM_PREPARACAO', 'DISPONIVEL', 'RESERVADO', 'VENDIDO')),
    status_novo      VARCHAR(20)  NOT NULL CHECK (status_novo IN ('RECEBIDO', 'EM_PREPARACAO', 'DISPONIVEL', 'RESERVADO', 'VENDIDO')),
    motivo           VARCHAR(255),
    venda_id         UUID,
    ocorrido_em      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_historico_status_veiculo_veiculo_id ON historico_status_veiculo (veiculo_id, ocorrido_em);
