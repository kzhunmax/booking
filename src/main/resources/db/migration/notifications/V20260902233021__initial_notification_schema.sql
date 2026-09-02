CREATE SEQUENCE IF NOT EXISTS notifications_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE notifications
(
    id         BIGINT       NOT NULL,
    public_id  UUID         NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    recipient  VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    sent_at    TIMESTAMPTZ,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

ALTER TABLE notifications
    ADD CONSTRAINT uc_notifications_public UNIQUE (public_id);
