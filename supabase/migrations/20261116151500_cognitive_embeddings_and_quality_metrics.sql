-- Cognitive Memory Upgrades: Vector Space Versioning, HNSW Index & Measurable Evolution
-- Non-destructive: preserves all existing rows in public.memories, public.conversations, and public.cognitive_evaluations.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Track embedding model & structured metadata on public.memories so vector spaces are never mixed
ALTER TABLE public.memories
  ADD COLUMN IF NOT EXISTS embedding_model text NOT NULL DEFAULT 'none',
  ADD COLUMN IF NOT EXISTS metadata jsonb NOT NULL DEFAULT '{}'::jsonb;

-- Preserve accurate attribution for any pre-existing row that already stored a vector
UPDATE public.memories
SET embedding_model = 'deterministic_ngram_384'
WHERE embedding IS NOT NULL
  AND embedding_model = 'none';

-- Partial HNSW index for fast cosine similarity over active embedded memories
CREATE INDEX IF NOT EXISTS idx_memories_embedding_hnsw
  ON public.memories USING hnsw (embedding vector_cosine_ops)
  WHERE is_active = true AND embedding IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_memories_active_emb_model
  ON public.memories (is_active, embedding_model);

-- 2. Extend public.cognitive_evaluations for cross-family LLM benchmarks
ALTER TABLE public.cognitive_evaluations
  ADD COLUMN IF NOT EXISTS cross_family_models text[] NOT NULL DEFAULT '{}'::text[],
  ADD COLUMN IF NOT EXISTS details_version integer NOT NULL DEFAULT 1;
