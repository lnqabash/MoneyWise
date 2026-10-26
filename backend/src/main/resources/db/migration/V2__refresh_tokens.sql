-- Refresh tokens for rotating, revocable sessions (auth hardening, docs/09).
-- The raw token is never stored — only a SHA-256 hash.

CREATE TABLE refresh_tokens (
    token_id       UUID         NOT NULL PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    token_hash     VARCHAR(80)  NOT NULL UNIQUE,
    revoked        BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at     TIMESTAMP    NOT NULL,
    created_at     TIMESTAMP    NOT NULL
);
CREATE INDEX ix_refresh_user ON refresh_tokens (user_id);
