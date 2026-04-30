CREATE TABLE tickets (
    id               UUID        NOT NULL,
    title            VARCHAR(500) NOT NULL,
    content          TEXT        NOT NULL,
    status           VARCHAR(50) NOT NULL,
    priority         VARCHAR(50) NOT NULL,
    customer_id      VARCHAR(255) NOT NULL,
    category         VARCHAR(100),
    assigned_agent_id   VARCHAR(255),
    assigned_agent_name VARCHAR(255),
    resolution_note  TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at      TIMESTAMPTZ,

    CONSTRAINT pk_tickets PRIMARY KEY (id)
);

CREATE INDEX idx_tickets_customer_id ON tickets (customer_id);
CREATE INDEX idx_tickets_status      ON tickets (status);
CREATE INDEX idx_tickets_agent_id    ON tickets (assigned_agent_id);
