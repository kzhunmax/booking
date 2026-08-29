CREATE SEQUENCE IF NOT EXISTS payments_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payments
(
    id                BIGINT         NOT NULL,
    public_id         UUID           NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    booking_id        UUID           NOT NULL,
    user_id           UUID,
    amount            DECIMAL(19, 4) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    status            VARCHAR(255)   NOT NULL,
    idempotency_key   UUID           NOT NULL,
    gateway_reference VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_public UNIQUE (public_id);
