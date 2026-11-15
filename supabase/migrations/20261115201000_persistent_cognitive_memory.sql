-- Persistent Cognitive Memory Architecture (Model-Agnostic & Non-Destructive)
-- Preserves all existing records in public.memories, public.conversations, public.aprendizado_ia, and public.documents.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Extend public.memories with metadata, versioning, deduplication, contradiction resolution, and FTS
ALTER TABLE public.memories
  ADD COLUMN IF NOT EXISTS memory_uuid uuid NOT NULL DEFAULT gen_random_uuid(),
  ADD COLUMN IF NOT EXISTS origem text NOT NULL DEFAULT 'legacy_session',
  ADD COLUMN IF NOT EXISTS categoria text NOT NULL DEFAULT 'episodic',
  ADD COLUMN IF NOT EXISTS confianca numeric(4,3) NOT NULL DEFAULT 0.850,
  ADD COLUMN IF NOT EXISTS versao integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS content_hash text,
  ADD COLUMN IF NOT EXISTS superseded_by bigint REFERENCES public.memories(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS embedding vector(384);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'memories_categoria_check'
  ) THEN
    ALTER TABLE public.memories
      ADD CONSTRAINT memories_categoria_check
      CHECK (categoria IN ('episodic', 'semantic', 'procedural', 'strategic', 'preference', 'correction', 'conversation'));
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'memories_confianca_check'
  ) THEN
    ALTER TABLE public.memories
      ADD CONSTRAINT memories_confianca_check
      CHECK (confianca >= 0 AND confianca <= 1);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'memories' AND column_name = 'search_tsv'
  ) THEN
    ALTER TABLE public.memories
      ADD COLUMN search_tsv tsvector
      GENERATED ALWAYS AS (to_tsvector('portuguese', coalesce(content, ''))) STORED;
  END IF;
END $$;

-- Backfill content_hash on existing rows without altering existing content or timestamps
UPDATE public.memories
SET content_hash = encode(digest(lower(regexp_replace(trim(coalesce(content, '')), '\s+', ' ', 'g')), 'sha256'), 'hex')
WHERE content_hash IS NULL;

CREATE INDEX IF NOT EXISTS idx_memories_active_cat_created
  ON public.memories (is_active, categoria, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_memories_content_hash
  ON public.memories (content_hash);

CREATE INDEX IF NOT EXISTS idx_memories_search_tsv
  ON public.memories USING GIN (search_tsv);

-- 2. Extend public.conversations for session tracking and model attribution
ALTER TABLE public.conversations
  ADD COLUMN IF NOT EXISTS session_id text,
  ADD COLUMN IF NOT EXISTS model_used text,
  ADD COLUMN IF NOT EXISTS metadata jsonb NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS idx_conversations_session_created
  ON public.conversations (session_id, created_at DESC);

-- 3. Measurable Evolution & Evaluation Table
CREATE TABLE IF NOT EXISTS public.cognitive_evaluations (
  id bigserial PRIMARY KEY,
  created_at timestamptz NOT NULL DEFAULT now(),
  model_used text NOT NULL,
  suite_name text NOT NULL,
  total_cases integer NOT NULL CHECK (total_cases >= 0),
  passed_cases integer NOT NULL CHECK (passed_cases >= 0),
  retrieval_accuracy numeric(5,4) NOT NULL CHECK (retrieval_accuracy >= 0 AND retrieval_accuracy <= 1),
  validated_memories_count integer NOT NULL DEFAULT 0,
  corrected_responses_count integer NOT NULL DEFAULT 0,
  repeated_errors_count integer NOT NULL DEFAULT 0,
  details jsonb NOT NULL DEFAULT '{}'::jsonb
);

ALTER TABLE public.cognitive_evaluations ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'cognitive_evaluations' AND policyname = 'service_role_all_cognitive_evaluations'
  ) THEN
    CREATE POLICY service_role_all_cognitive_evaluations
      ON public.cognitive_evaluations
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;
END $$;
