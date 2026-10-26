-- Community Savings Circles (digitised stokvels / chamas / ROSCAs)
-- See docs/16-differentiators.md §3. Money amounts in integer minor units.

CREATE TABLE circles (
    circle_id           UUID         NOT NULL PRIMARY KEY,
    organiser_user_id   UUID         NOT NULL REFERENCES users (user_id),
    name                VARCHAR(120) NOT NULL,
    type                VARCHAR(8)   NOT NULL DEFAULT 'rosca',  -- rosca | asca
    contribution_minor  BIGINT       NOT NULL CHECK (contribution_minor > 0),
    currency            VARCHAR(3)   NOT NULL,
    cadence             VARCHAR(10)  NOT NULL DEFAULT 'weekly',  -- weekly | biweekly | monthly
    member_limit        INT          NOT NULL DEFAULT 12,
    current_cycle_no    INT          NOT NULL DEFAULT 0,
    status              VARCHAR(10)  NOT NULL DEFAULT 'forming', -- forming | active | completed
    created_at          TIMESTAMP    NOT NULL
);
CREATE INDEX ix_circles_organiser ON circles (organiser_user_id);

CREATE TABLE circle_members (
    member_id           UUID         NOT NULL PRIMARY KEY,
    circle_id           UUID         NOT NULL REFERENCES circles (circle_id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES users (user_id),
    payout_position     INT          NOT NULL,                  -- rotation order (1-based)
    status              VARCHAR(10)  NOT NULL DEFAULT 'active',  -- invited | active | left
    joined_at           TIMESTAMP    NOT NULL,
    CONSTRAINT uq_circle_user UNIQUE (circle_id, user_id),
    CONSTRAINT uq_circle_position UNIQUE (circle_id, payout_position)
);
CREATE INDEX ix_members_circle ON circle_members (circle_id);
CREATE INDEX ix_members_user ON circle_members (user_id);

CREATE TABLE circle_cycles (
    cycle_id            UUID         NOT NULL PRIMARY KEY,
    circle_id           UUID         NOT NULL REFERENCES circles (circle_id) ON DELETE CASCADE,
    cycle_number        INT          NOT NULL,
    payout_member_id    UUID         REFERENCES circle_members (member_id),  -- ROSCA recipient
    pot_minor           BIGINT       NOT NULL DEFAULT 0,
    status              VARCHAR(8)   NOT NULL DEFAULT 'open',    -- open | paid | closed
    created_at          TIMESTAMP    NOT NULL,
    CONSTRAINT uq_circle_cycle UNIQUE (circle_id, cycle_number)
);
CREATE INDEX ix_cycles_circle ON circle_cycles (circle_id);

CREATE TABLE circle_contributions (
    contribution_id     UUID         NOT NULL PRIMARY KEY,
    cycle_id            UUID         NOT NULL REFERENCES circle_cycles (cycle_id) ON DELETE CASCADE,
    member_id           UUID         NOT NULL REFERENCES circle_members (member_id),
    amount_minor        BIGINT       NOT NULL CHECK (amount_minor > 0),
    status              VARCHAR(8)   NOT NULL DEFAULT 'paid',    -- pending | paid | late | missed
    paid_at             TIMESTAMP,
    CONSTRAINT uq_cycle_member UNIQUE (cycle_id, member_id)
);
CREATE INDEX ix_contrib_cycle ON circle_contributions (cycle_id);
