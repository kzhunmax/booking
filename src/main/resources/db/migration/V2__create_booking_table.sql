CREATE SEQUENCE booking_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS booking
(
    id             BIGINT       NOT NULL,
    public_id      UUID         NOT NULL,
    resource_id    UUID         NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_name  VARCHAR(255) NOT NULL,
    starts_at      TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_booking PRIMARY KEY (id),

    CONSTRAINT uk_booking_public_id UNIQUE (public_id)
);

CREATE INDEX idx_booking_resource_time
    ON booking (resource_id, starts_at, ends_at)
    WHERE status != 'CANCELLED';
