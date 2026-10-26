-- ==============================================================================
-- System Rezerwacji Wizyt i Usług (Appointment & Service Booking System)
-- Kompletny Zrzut Bazy Danych PostgreSQL (Struktura DDL + Dane Początkowe DML)
-- Data generowania: 2026-10-26
-- ==============================================================================

-- 1. Rozszerzenia bazy danych
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 2. Sekwencje identyfikatorów (krok INCREMENT BY 50 dla optymalizatora Hibernate pooled-lo)
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS resources_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS bookings_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS payments_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS notifications_seq START WITH 1 INCREMENT BY 50;

-- 3. Definicje tabel (DDL)

-- Tabela: USERS
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL,
    public_id     UUID         NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email),
    CONSTRAINT uc_users_public UNIQUE (public_id)
);

-- Tabela: RESOURCES
CREATE TABLE IF NOT EXISTS resources (
    id             BIGINT         NOT NULL,
    public_id      UUID           NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    status         VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    price_per_hour DECIMAL(19, 4) NOT NULL DEFAULT 0.01,
    price_currency VARCHAR(3)     NOT NULL DEFAULT 'USD',
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_resources PRIMARY KEY (id),
    CONSTRAINT uk_resources_public_id UNIQUE (public_id)
);

-- Częściowy indeks unikalny dla aktywnych zasobów (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS uk_resources_name_lower
    ON resources (LOWER(name))
    WHERE status <> 'ARCHIVED';

-- Tabela: BOOKINGS
CREATE TABLE IF NOT EXISTS bookings (
    id             BIGINT         NOT NULL,
    public_id      UUID           NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    resource_id    UUID           NOT NULL,
    customer_email VARCHAR(255)   NOT NULL,
    customer_name  VARCHAR(255)   NOT NULL,
    starts_at      TIMESTAMPTZ    NOT NULL,
    ends_at        TIMESTAMPTZ    NOT NULL,
    status         VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    total_amount   DECIMAL(19, 4) NOT NULL DEFAULT 0.01,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_booking PRIMARY KEY (id),
    CONSTRAINT uk_booking_public_id UNIQUE (public_id),
    CONSTRAINT no_overlapping_bookings
        EXCLUDE USING gist (
            resource_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        )
        WHERE (status != 'CANCELLED')
);

CREATE INDEX IF NOT EXISTS idx_booking_resource_time
    ON bookings (resource_id, starts_at, ends_at)
    WHERE status != 'CANCELLED';

-- Tabela: PAYMENTS
CREATE TABLE IF NOT EXISTS payments (
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
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uc_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uc_payments_public UNIQUE (public_id)
);

-- Tabela: NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL,
    public_id  UUID         NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    recipient  VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    sent_at    TIMESTAMPTZ,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uc_notifications_public UNIQUE (public_id)
);

-- ==============================================================================
-- 4. Dane początkowe (Seed Data DML)
-- ==============================================================================

-- 4.1. Użytkownicy (Hasła zahaszowane algorytmem BCrypt)
-- Hasło dla admin@admin.com: "admin"
-- Hasło dla klientów: "Password@123"
INSERT INTO users (id, public_id, version, email, password_hash, name, role, status, created_at, updated_at)
VALUES
(1, 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 0, 'admin@admin.com', '{bcrypt}$2a$10$bOz71ZmoYjc0.PgrX9MQbu6j.lV9KJvKaESwG8LnPJh3aZKJ1bye2', 'Administrator Systemu', 'ADMIN', 'ACTIVE', NOW() - INTERVAL '30 days', NOW()),
(2, 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 0, 'jan.kowalski@example.com', '{bcrypt}$2a$10$CurodnRYYSW/DuokATzl..JjCILNA1bn5s1LtzW/0VUM/tZRnkc8.', 'Jan Kowalski', 'CUSTOMER', 'ACTIVE', NOW() - INTERVAL '20 days', NOW()),
(3, 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 0, 'anna.nowak@example.com', '{bcrypt}$2a$10$CurodnRYYSW/DuokATzl..JjCILNA1bn5s1LtzW/0VUM/tZRnkc8.', 'Anna Nowak', 'CUSTOMER', 'ACTIVE', NOW() - INTERVAL '15 days', NOW())
ON CONFLICT (email) DO NOTHING;

-- 4.2. Usługi / Zasoby
INSERT INTO resources (id, public_id, version, name, description, status, price_per_hour, price_currency, created_at, updated_at)
VALUES
(1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 0, 'Dr n. med. Anna Kowalska - Kardiolog', 'Konsultacja specjalistyczna kardiologiczna wraz z badaniem EKG spoczynkowym', 'ACTIVE', 250.0000, 'PLN', NOW() - INTERVAL '30 days', NOW()),
(2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 0, 'Lek. stom. Michał Wiśniewski', 'Stomatologia zachowawcza, estetyczna oraz przegląd uzębienia', 'ACTIVE', 200.0000, 'PLN', NOW() - INTERVAL '30 days', NOW()),
(3, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 0, 'Gabinet Zabiegowy A1 - USG Doppler', 'Diagnostyka ultrasonograficzna naczyń krwionośnych kończyn dolnych', 'ACTIVE', 180.0000, 'PLN', NOW() - INTERVAL '25 days', NOW()),
(4, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 0, 'Fizjoterapia i Masaż Leczniczy', 'Indywidualna terapia manualna oraz rehabilitacja pourazowa', 'ACTIVE', 140.0000, 'PLN', NOW() - INTERVAL '20 days', NOW()),
(5, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 0, 'Dawna Usługa Laboratoryjna', 'Usługa zarchiwizowana - archiwum historyczne', 'ARCHIVED', 80.0000, 'PLN', NOW() - INTERVAL '60 days', NOW() - INTERVAL '10 days')
ON CONFLICT (public_id) DO NOTHING;

-- 4.3. Rezerwacje Wizyt (Przykłady różnych statusów)
INSERT INTO bookings (id, public_id, version, resource_id, customer_email, customer_name, starts_at, ends_at, status, total_amount, currency, created_at, updated_at)
VALUES
-- Rezerwacja 1: Potwierdzona i opłacona (jutro 10:00 - 11:00)
(1, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'jan.kowalski@example.com', 'Jan Kowalski', (CURRENT_DATE + INTERVAL '1 day 10 hours'), (CURRENT_DATE + INTERVAL '1 day 11 hours'), 'CONFIRMED', 250.0000, 'PLN', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
-- Rezerwacja 2: Oczekująca na płatność (jutro 12:00 - 13:00)
(2, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 0, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'anna.nowak@example.com', 'Anna Nowak', (CURRENT_DATE + INTERVAL '1 day 12 hours'), (CURRENT_DATE + INTERVAL '1 day 13 hours'), 'PENDING', 200.0000, 'PLN', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
-- Rezerwacja 3: Anulowana przez klienta (pojutrze 14:00 - 15:00)
(3, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', 1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'jan.kowalski@example.com', 'Jan Kowalski', (CURRENT_DATE + INTERVAL '2 days 14 hours'), (CURRENT_DATE + INTERVAL '2 days 15 hours'), 'CANCELLED', 180.0000, 'PLN', NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day')
ON CONFLICT (public_id) DO NOTHING;

-- 4.4. Transakcje Płatnicze (Stripe API)
INSERT INTO payments (id, public_id, version, booking_id, user_id, amount, currency, status, idempotency_key, gateway_reference, created_at, updated_at)
VALUES
(1, 'd1eebc99-9c0b-4ef8-bb6d-6bb9bd380a31', 0, 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 250.0000, 'PLN', 'SUCCEEDED', 'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a41', 'pi_3MtwBwLkdIwHu7ix28a3tqPa', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (public_id) DO NOTHING;

-- 4.5. Powiadomienia (WebSocket / Email)
INSERT INTO notifications (id, public_id, type, recipient, subject, body, status, created_at, sent_at)
VALUES
(1, 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a51', 'BOOKING_CONFIRMED', 'jan.kowalski@example.com', 'Potwierdzenie rezerwacji wizyty', 'Twoja wizyta u dr Anny Kowalskiej została opłacona i potwierdzona.', 'SENT', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(2, 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a52', 'BOOKING_CANCELLED', 'jan.kowalski@example.com', 'Anulowanie rezerwacji wizyty', 'Rezerwacja wizyty w Gabinecie Zabiegowym A1 została pomyślnie anulowana.', 'SENT', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (public_id) DO NOTHING;

-- Zsynchronizowanie sekwencji
SELECT setval('users_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM users));
SELECT setval('resources_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM resources));
SELECT setval('bookings_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM bookings));
SELECT setval('payments_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM payments));
SELECT setval('notifications_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM notifications));
