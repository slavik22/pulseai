CREATE TABLE ai_suggestions (
    id                   UUID        NOT NULL,
    ticket_id            VARCHAR(255) NOT NULL,
    suggested_response   TEXT        NOT NULL,
    confidence_score     DOUBLE PRECISION NOT NULL,
    feedback_status      VARCHAR(50) NOT NULL,
    agent_edited_response TEXT,
    source_article_ids   TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    feedback_at          TIMESTAMPTZ,

    CONSTRAINT pk_ai_suggestions PRIMARY KEY (id)
);

CREATE INDEX idx_ai_suggestions_ticket_id ON ai_suggestions (ticket_id);
