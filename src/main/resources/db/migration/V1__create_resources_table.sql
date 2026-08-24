CREATE SEQUENCE resources_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS resources
(
    id          BIGINT       NOT NULL,
    public_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_resources PRIMARY KEY (id),

    CONSTRAINT uk_resources_public_id UNIQUE (public_id)
);

CREATE UNIQUE INDEX uk_resources_name_lower
    ON resources (LOWER(name))
    WHERE status <> 'ARCHIVED';

