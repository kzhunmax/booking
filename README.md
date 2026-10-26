# Booking App

Modular monolith REST API for resource booking and payment processing built with **Java 25** and **Spring Boot**.

---

## Tech Stack

- **Language & Framework:** Java 25, Spring Boot 4
- **Security:** Spring Security, JWT (JJWT)
- **API Docs:** Springdoc OpenAPI (Swagger UI at `/swagger-ui/index.html`)
- **Database & Migrations:** PostgreSQL, Flyway
- **Tooling & Quality:** Spotless (Palantir Java Format), Checkstyle, Error Prone, JaCoCo
- **Testing:** JUnit 5, AssertJ, Mockito, ArchUnit, Testcontainers
- **Local Infra:** Docker Compose (PostgreSQL, Mailpit)

---

## Domain Modules

- **`auth`** — User registration, authentication, JWT issuing & validation, user status management.
- **`resource`** — Inventory of bookable resources (pricing, status, availability).
- **`booking`** — Reservation lifecycle, schedule collision detection, available slots query.
- **`payment`** — Payment handling with idempotency key support.
- **`notification`** — Event notifications (emails dispatched via SMTP, plus WebSocket broadcast on `/topic/bookings`).

---

## Quick Start

### 1. Prerequisites
- **JDK 25** installed
- **Docker & Docker Compose** running

### 2. Setup Git Hooks
Configure the repository pre-commit hooks:
```bash
./gradlew installGitHooks
```

### 3. Start Local Infrastructure
Run PostgreSQL and Mailpit in the background:
```bash
docker compose up -d
```
- **PostgreSQL:** `localhost:5432` (DB: `booking`, user/pass: `postgres`/`postgres`)
- **Mailpit Web UI:** [http://localhost:8025](http://localhost:8025) (SMTP on `1025`)

### 4. Run the Application
Start with the `dev` Spring profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Emails sent by the app (booking confirmed / cancelled) can be viewed in Mailpit. By default the app sends mail to `localhost:1025` (override with `MAIL_HOST` / `MAIL_PORT`).

*Default dev admin:* `admin@admin.com` / `admin`

---

## Useful Commands

| Task | Command | Description |
|------|---------|-------------|
| **Run unit tests** | `./gradlew test` | Fast tests excluding integration suite |
| **Run integration tests** | `./gradlew integrationTest` | Full context tests via Testcontainers |
| **Format code** | `./gradlew spotlessApply` | Applies Palantir Java Format & trims imports |
| **Code check** | `./gradlew check` | Runs Spotless check, Checkstyle & integration tests |
| **Coverage report** | `./gradlew test jacocoTestReport` | Generates HTML report in `build/reports/jacoco/test/html/` |
| **Stop local infra** | `docker compose down` | Stops and removes containers |
