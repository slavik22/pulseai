-- Enable pgvector extension
-- WHY? ai-gateway-service stores embeddings in Postgres using pgvector.
-- One DB, familiar ops, no separate vector database to maintain.
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
