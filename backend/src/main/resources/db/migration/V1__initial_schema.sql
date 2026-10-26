-- MoneyMate — initial schema (thin vertical slice)
-- Subset of docs/06-database-design.md. Money stored as integer minor units.
-- Written to be portable across H2 (PostgreSQL mode) and PostgreSQL.

CREATE TABLE users (
    user_id             UUID            NOT NULL PRIMARY KEY,
    phone_e164          VARCHAR(20)     NOT NULL UNIQUE,
    display_name        VARCHAR(120),
    preferred_language  VARCHAR(5)      NOT NULL DEFAULT 'en',
    preferred_channel   VARCHAR(10)     NOT NULL DEFAULT 'app',
    persona_segment     VARCHAR(30),
    income_pattern      VARCHAR(20),
    currency            VARCHAR(3)      NOT NULL DEFAULT 'ZAR',
    kyc_level           VARCHAR(10)     NOT NULL DEFAULT 'lite',
    consent_data_ai     BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_users_segment ON users (persona_segment);

-- One-time password challenges for phone auth.
CREATE TABLE otp_challenges (
    challenge_id        UUID            NOT NULL PRIMARY KEY,
    phone_e164          VARCHAR(20)     NOT NULL,
    code_hash           VARCHAR(100)    NOT NULL,
    attempts            INT             NOT NULL DEFAULT 0,
    consumed            BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at          TIMESTAMP       NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_otp_phone ON otp_challenges (phone_e164);

CREATE TABLE income (
    income_id           UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    amount_minor        BIGINT          NOT NULL CHECK (amount_minor >= 0),
    currency            VARCHAR(3)      NOT NULL,
    source              VARCHAR(20)     NOT NULL,
    capture_method      VARCHAR(10)     NOT NULL,
    is_business         BOOLEAN         NOT NULL DEFAULT FALSE,
    received_on         DATE            NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_income_user_date ON income (user_id, received_on);

CREATE TABLE expenses (
    expense_id          UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    amount_minor        BIGINT          NOT NULL CHECK (amount_minor > 0),
    currency            VARCHAR(3)      NOT NULL,
    category            VARCHAR(40)     NOT NULL,
    is_business         BOOLEAN         NOT NULL DEFAULT FALSE,
    capture_method      VARCHAR(10)     NOT NULL,
    spent_on            DATE            NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_expenses_user_date ON expenses (user_id, spent_on);
CREATE INDEX ix_expenses_user_cat ON expenses (user_id, category);

CREATE TABLE savings_goals (
    goal_id             UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    name                VARCHAR(120)    NOT NULL,
    goal_type           VARCHAR(20)     NOT NULL DEFAULT 'general',
    target_minor        BIGINT          NOT NULL CHECK (target_minor > 0),
    saved_minor         BIGINT          NOT NULL DEFAULT 0 CHECK (saved_minor >= 0),
    currency            VARCHAR(3)      NOT NULL,
    target_date         DATE,
    rule_type           VARCHAR(15)     NOT NULL DEFAULT 'manual',
    rule_amount_minor   BIGINT,
    soft_locked         BOOLEAN         NOT NULL DEFAULT TRUE,
    status              VARCHAR(10)     NOT NULL DEFAULT 'active',
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_goals_user_status ON savings_goals (user_id, status);

CREATE TABLE transactions (
    txn_id              UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            NOT NULL REFERENCES users (user_id),
    goal_id             UUID            REFERENCES savings_goals (goal_id) ON DELETE SET NULL,
    income_id           UUID,
    expense_id          UUID,
    direction           VARCHAR(6)      NOT NULL CHECK (direction IN ('credit', 'debit')),
    type                VARCHAR(15)     NOT NULL,
    amount_minor        BIGINT          NOT NULL CHECK (amount_minor > 0),
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(10)     NOT NULL,
    idempotency_key     VARCHAR(80)     NOT NULL UNIQUE,
    occurred_at         TIMESTAMP       NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_txn_user_time ON transactions (user_id, occurred_at);
CREATE INDEX ix_txn_goal ON transactions (goal_id);

CREATE TABLE scam_reports (
    report_id           UUID            NOT NULL PRIMARY KEY,
    user_id             UUID            REFERENCES users (user_id) ON DELETE SET NULL,
    channel             VARCHAR(10)     NOT NULL,
    content_hash        VARCHAR(80)     NOT NULL,
    risk_level          VARCHAR(6)      NOT NULL,
    risk_score          DOUBLE PRECISION NOT NULL,
    red_flags           CLOB,
    model_version       VARCHAR(40)     NOT NULL,
    user_action         VARCHAR(12),
    created_at          TIMESTAMP       NOT NULL
);
CREATE INDEX ix_scam_user ON scam_reports (user_id);
