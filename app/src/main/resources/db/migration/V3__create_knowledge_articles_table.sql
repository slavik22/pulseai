-- Enable pgvector extension (needed for LangChain4j embedding store)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_articles (
    id                  UUID        NOT NULL,
    title               VARCHAR(500) NOT NULL,
    content             TEXT        NOT NULL,
    category            VARCHAR(100),
    verified_by_expert  BOOLEAN     NOT NULL DEFAULT FALSE,
    indexed             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    indexed_at          TIMESTAMPTZ,

    CONSTRAINT pk_knowledge_articles PRIMARY KEY (id)
);

CREATE INDEX idx_knowledge_articles_category ON knowledge_articles (category);
CREATE INDEX idx_knowledge_articles_indexed  ON knowledge_articles (indexed);
