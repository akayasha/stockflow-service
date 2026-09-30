# StockFlow Submission Checklist

This file maps the take-home requirements to the current implementation and
the commands used to verify it.

## Core coverage

| Area | Status | Notes |
|---|---:|---|
| Register/login/logout | Done | JWT auth, bcrypt password hashing, generic auth errors. |
| Protected inventory/invoice API | Done | `/api/**` requires auth; unauthenticated requests return `401`. |
| Data isolation / shared catalog | Done | STAFF data remains owner-scoped; seeded ADMIN owns shared products visible to all staff. |
| Products CRUD/search/pagination | Done | Delete is blocked with `409` when referenced by an invoice. |
| Invoice create/list/detail | Done | Server calculates totals and snapshots product name/unit price. |
| Draft line item editing | Done | Backend `PATCH /api/invoices/{id}/items`; Angular route `/invoices/:id/edit`. |
| Stock guard | Done | Over-stock is rejected during create/edit and rechecked atomically during issue. |
| Issue/cancel stock movement | Done | Issue decrements stock; cancelling ISSUED restores stock. |
| Status transitions | Done | Enforced server-side by `InvoiceStateMachine`. |
| Money handling | Done | Backend uses `BigDecimal` and PostgreSQL `DECIMAL(19,2)`. |
| Env-driven config | Done | DB/JWT/tax/seed/server port read from environment variables with safe defaults. |
| Seed data | Done | Demo user: `demo@stockflow.dev` / `Demo1234!`. |
| API docs | Done | Swagger UI at `/swagger-ui.html`. |
| Error shape | Done | Consistent `{ "error": { ... } }` envelope with field errors. |
| Angular UI | Done | Auth pages, product CRUD, invoice create/list/detail/actions, DRAFT item edit. |

## Admin / staff shortcut

- Register creates `STAFF` users only.
- `V5__seed_admin_user.sql` seeds the canonical `ADMIN` account.
- ADMIN-owned products are treated as the shared catalog.
- STAFF can view and invoice ADMIN products but cannot edit/delete them.
- ADMIN can list/view all invoices.

## Verification commands

Backend:

```powershell
.\mvnw.cmd -q test
```

Full backend integration tests require Docker/Testcontainers:

```powershell
$env:RUN_INTEGRATION_TESTS='true'
.\mvnw.cmd -q verify
```

Frontend:

```powershell
cd D:\works\stockflow-web
npm run build
```

## Known limitations / deliberate cuts

- No refresh-token rotation; logout invalidates existing JWTs via token version.
- No login rate limiting.
- No invoice PDF/print view.
- No CI pipeline yet.
- Integration tests are skipped when Docker is unavailable.
- Invoice edit UI is intentionally limited to line items because the backend
  requirement only allows DRAFT line item editing.

## Pre-submit reminders

- Start Docker Desktop and run full backend verify once before submission.
- Confirm no real `.env` or secrets are committed.
- Make meaningful Git commits instead of one large final commit.
- Include both backend and Angular run instructions in the final repository
  README or submission notes.
