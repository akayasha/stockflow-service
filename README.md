# StockFlow

A minimal **inventory & invoicing** app for the StockFlow take-home test.
Backend: Java 21, Spring Boot 4.1, PostgreSQL, JWT auth, Flyway migrations.
Frontend: Angular, reactive forms, route guards, and a small Tailwind UI.

```
+---------+      register / login     +-------------+
|  User   |  -----------------------> |  JWT token  |
+---------+                           +-------------+
     |
     |  Authorization: Bearer <token>
     v
+-----------------------+
|  Spring Boot REST API |
+-----------------------+
     |
     |  JPA + Hibernate + Flyway
     v
+-----------------------+
|     PostgreSQL 16     |
+-----------------------+
```

---

## Quick start

You need **JDK 21+**, **Maven 3.9+**, **Node.js 22+**, and a reachable
**PostgreSQL 14+**.

### Backend

```bash
# 1. Start Postgres (or use an existing one)
docker compose up -d postgres

# 2. Copy the sample env file and tweak if needed
cp .env.example .env

# 3. Run the backend
mvn spring-boot:run
```

The server listens on `http://localhost:8080`. Schema migrations run
automatically on first boot.

### Frontend

The Angular app lives in a separate folder:

```bash
cd D:\works\stockflow-web
npm install
npm start
```

The frontend runs on Angular's dev server and proxies API calls to
`http://localhost:8080` through `proxy.conf.json`.

### Demo credentials

| Field    | Value                |
|----------|----------------------|
| Email    | `demo@stockflow.dev` |
| Password | `Demo1234!`          |
| Role     | `ADMIN`              |

The admin user is seeded by Flyway migration `V5__seed_admin_user.sql`.
`DemoDataSeeder` then adds the demo products under this admin account when
`SEED_DEMO_DATA=true`.

### Admin / staff behavior

- Registration always creates `STAFF` users.
- The seeded `ADMIN` is created by Flyway migration
  `V5__seed_admin_user.sql`.
- The seeded `ADMIN` owns the shared product catalog.
- STAFF users can see and invoice ADMIN-owned products, but cannot edit/delete
  those shared products.
- STAFF users may still create their own private products.
- ADMIN can list/view all invoices. Invoice mutation actions remain staff-owned
  in the UI to avoid accidentally changing another user's invoice.

### Simple demo flow

1. Start backend and frontend.
2. Login as admin with `demo@stockflow.dev` / `Demo1234!`.
3. Confirm the seeded products are visible in **Products**. These are the
   shared catalog products.
4. Register a new staff account from the frontend.
5. Login as the staff account.
6. Open **Products**: staff can see shared admin products, but they are marked
   as shared and cannot be edited/deleted.
7. Create an invoice using one or more shared products.
8. Issue the invoice: stock is decremented atomically.
9. Login as admin again and open **Invoices**: admin can view all invoices.

### Use cases

| Actor | Use case |
|---|---|
| ADMIN | Login with seeded admin account. |
| ADMIN | Maintain shared catalog products seeded under the admin account. |
| ADMIN | View all invoices created by staff. |
| STAFF | Register and login with email/password. |
| STAFF | View shared admin catalog products. |
| STAFF | Create, update, and delete their own products. |
| STAFF | Create invoices using shared admin products and/or own products. |
| STAFF | Issue invoices to decrement stock. |
| STAFF | Cancel issued invoices to restore stock. |
| STAFF | Edit line items while invoice is still DRAFT. |

### Backend / frontend folders

This repository contains the backend. The Angular frontend is in:

```text
D:\works\stockflow-web
```

Run order:

1. Start PostgreSQL.
2. Start backend on `http://localhost:8080`.
3. Start Angular on `http://localhost:4200`.

---

## API at a glance

| Method | Path                              | Auth | Purpose |
|--------|-----------------------------------|------|---------|
| POST   | `/api/auth/register`              | -    | Create a user |
| POST   | `/api/auth/login`                 | -    | Exchange credentials for a JWT |
| POST   | `/api/auth/login-admin`           | -    | Login as seeded ADMIN |
| POST   | `/api/auth/logout`                | yes  | Invalidate the caller's tokens |
| GET    | `/api/products`                   | yes  | List own + ADMIN shared products |
| POST   | `/api/products`                   | yes  | Create a product |
| GET    | `/api/products/{id}`              | yes  | Fetch one product |
| PUT    | `/api/products/{id}`              | yes  | Update a product |
| DELETE | `/api/products/{id}`              | yes  | Delete own product (409 if referenced) |
| GET    | `/api/invoices`                   | yes  | List invoices; ADMIN sees all |
| POST   | `/api/invoices`                   | yes  | Create a DRAFT invoice |
| GET    | `/api/invoices/{id}`              | yes  | Fetch one invoice with totals |
| PATCH  | `/api/invoices/{id}/items`        | yes  | Replace line items (DRAFT only) |
| POST   | `/api/invoices/{id}/issue`        | yes  | DRAFT to ISSUED, decrements stock |
| POST   | `/api/invoices/{id}/pay`          | yes  | ISSUED to PAID |
| POST   | `/api/invoices/{id}/cancel`       | yes  | DRAFT or ISSUED to CANCELLED |
| GET    | `/v3/api-docs`                    | -    | OpenAPI JSON |
| GET    | `/swagger-ui.html`                | -    | Interactive API explorer |

Live docs: <http://localhost:8080/swagger-ui.html>

### Error envelope

Every error response uses the same JSON shape:

```json
{
  "error": {
    "code": "STOCK_INSUFFICIENT",
    "message": "Not enough stock for product ''Buku Tulis'' (have 2, need 5)",
    "status": 409
  }
}
```

Field-level validation errors include a `fields` map:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "status": 422,
    "fields": {
      "email": "email must be a valid email address"
    }
  }
}
```

| Status | When |
|--------|------|
| 401    | Missing or invalid credentials |
| 403    | Authenticated but not allowed |
| 404    | Resource not found, or not owned by the caller |
| 409    | Conflict (stock guard, illegal status transition, unique constraint) |
| 422    | Bean validation failure |

---

## Running tests

```bash
# All tests (unit only when Docker is unavailable)
mvn test

# Full suite including integration tests (requires Docker)
RUN_INTEGRATION_TESTS=true mvn verify
```

The integration tests start a Postgres 16 container via Testcontainers and
exercise the API end-to-end. The flag is opt-in so a plain `mvn test` stays
green on any machine.

---

## Configuration

All settings are environment-driven. Defaults work with the bundled
`docker-compose.yml` Postgres.

| Variable                 | Default                                              | Purpose |
|--------------------------|------------------------------------------------------|---------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/stockflow`         | JDBC URL |
| `DB_USERNAME`            | `stockflow`                                          | DB user |
| `DB_PASSWORD`            | `stockflow`                                          | DB password |
| `SERVER_PORT`            | `8080`                                               | HTTP port |
| `JWT_SECRET`             | (placeholder)                                        | HS256 signing key (>= 32 bytes) |
| `JWT_EXPIRATION_MINUTES` | `1440`                                               | Access token lifetime |
| `TAX_RATE`               | `0.11`                                               | Applied to invoice subtotals |
| `SEED_DEMO_DATA`         | `true`                                               | Seed demo user and products |

Generate a real JWT secret with:

```bash
openssl rand -base64 48
```

---

## Tech stack and why

- **Java 21 + Spring Boot 4.1** - current LTS line, records and pattern
  matching keep the code dense.
- **Spring Data JPA + Hibernate 7** - FK constraints and indexed reads
  out of the box; `open-in-view: false` keeps transactions bounded.
- **Flyway** - every schema change is a numbered SQL file under
  `db/migration`. Reviewers can read the database top to bottom.
- **JWT (JJWT 0.12) + Spring Security 7** - stateless. A `tokenVersion`
  claim is bumped on logout so every previously issued token is rejected
  without a server-side blacklist.
- **BigDecimal + DECIMAL(19,2)** - money is never rounded through float.
  All arithmetic goes through `BigDecimal.add` / `multiply` with
  `RoundingMode.HALF_UP`.
- **Pessimistic lock on the invoice-number counter** - `INV-YYYY-NNNN`
  allocation is safe under concurrent submissions because the row is
  locked for the duration of the increment.
- **Pessimistic lock on `Product` during issue** - a `SELECT ... FOR
  UPDATE` per line item prevents oversell when two staff members issue
  invoices against the same product at the same time.
- **`@Version` optimistic lock on every entity** - automatic protection
  against lost updates for general edits.
- **BaseEntity + JPA Auditing** - `createdAt`, `updatedAt`, `createdBy`
  and `updatedBy` populated automatically by an `AuditorAware` that reads
  from the security context.
- **springdoc-openapi** - the live API contract is browsable at
  `/swagger-ui.html` during development.
- **Testcontainers + JUnit 5** - integration tests run against the same
  Postgres engine the app ships with.

---

## Project layout

```
src/main/java/com/stockflow
|-- StockFlowServicesApplication.java
|-- auth/                  register, login, logout
|-- common/
|   |-- base/              BaseEntity, AuditorAware
|   |-- error/             ApiException hierarchy + GlobalExceptionHandler
|   |-- page/              PageResponse envelope
|   |-- security/          JWT service, filter, current-user helper
|-- config/                Security, OpenAPI, CORS, AppProperties
|-- invoice/               entities, service, controller, state machine, numbering
|-- product/               entities, service, controller
|-- seed/                  DemoDataSeeder
|-- user/                  User entity, repository, UserDetailsService

src/main/resources
|-- application.properties
|-- db/migration/          V1..V5 Flyway SQL files

src/test/java/com/stockflow
|-- StockFlowIntegrationTest.java   (gated behind RUN_INTEGRATION_TESTS)
|-- auth/dto/AuthDtoValidationTest.java
|-- invoice/dto/InvoiceDtoValidationTest.java
|-- invoice/statemachine/InvoiceStateMachineTest.java
|-- product/dto/ProductRequestValidationTest.java
|-- testsupport/PostgresTestContainer.java
```

---

## Trade-offs and known limitations

- **No refresh tokens.** Logout invalidates all tokens for the user
  immediately via the `tokenVersion` claim. There is no rotation flow.
- **Role support is intentionally small.** Register creates STAFF users only.
  The seeded ADMIN owns the shared product catalog and can view all invoices.
  A fuller company/workspace model would be the next step in a production app.
- **No multi-currency.** Money is `DECIMAL(19,2)` and assumes one
  currency per deployment.
- **No PDF export, no email, no CI, no deploy.** These are listed in the
  take-home brief as "bonus only" and were intentionally cut.
- **Hard delete on products.** Deleting a product referenced by an
  invoice returns 409. Switching to soft delete is a one-column migration
  plus a `@Where` clause.
- **Optimistic locking retries.** The invoice flow already locks the
  product row pessimistically, so concurrent updates are serialised. The
  `@Version` counter is wired in for general use but the invoice flow
  does not currently retry on `OptimisticLockException`.

---

## What I would do with one more week

- Expose the stock-movement ledger at `/api/stock-movements` with
  pagination and CSV export.
- Wire `docker compose up` to also run the migrations, so a single
  command stands up the whole stack including schema.
- Add Spring Boot Actuator and a Grafana dashboard for the JWT
  token-issuance and stock-movement counters.
- Build a small Playwright suite that drives the (forthcoming) Angular
  frontend through the F1-F6 flows.
- Introduce role-based access control and a proper refresh-token rotation
  flow.

---

## AI usage

This project was scaffolded and refined with help from Codex (OpenAI). The
model produced the initial directory layout, the bulk of the DTO records,
the repository interfaces, the Flyway migrations and most of the service
implementations. Every piece was reviewed line-by-line before being kept.
In particular:

- The concurrency design (pessimistic lock on the invoice-number counter,
  pessimistic lock on each product row inside the issue transaction) was
  sketched manually before being typed out.
- The error envelope shape and HTTP status mapping were authored
  manually to match the take-home brief.
- The JWT `tokenVersion` invalidation strategy was chosen manually
  instead of asking for a Redis-based blacklist.

AI assistance was used for: generating `pom.xml`, `application.properties`,
`.env.example`, `docker-compose.yml`, the integration test skeleton, this
README and the demo credential pair.

## Time spent

About four hours of focused work in a single afternoon. Most of the time
went into the security wiring and the invoice state machine. The
auto-generated parts were quicker to produce than they would have been
to type, but every line was re-read and edited.
