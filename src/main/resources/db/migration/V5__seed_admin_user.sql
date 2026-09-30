-- V5: Seed the canonical admin user.
--
-- Register always creates STAFF users in the application layer. The only
-- built-in ADMIN account is this seeded demo user, which owns the shared
-- product catalog used by staff invoices.
INSERT INTO users (
    id,
    email,
    password_hash,
    role,
    token_version,
    created_at,
    updated_at,
    version
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo@stockflow.dev',
    '$2a$10$Z3/can7YzUooQU8i5bKwYenFE7w8zOAemOSjxMRdMWY6kiBPnPWRm',
    'ADMIN',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (email) DO UPDATE
SET role = 'ADMIN',
    password_hash = EXCLUDED.password_hash,
    updated_at = CURRENT_TIMESTAMP;
