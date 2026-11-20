-- ============================================================================
-- FASE 1 (CUSTO ZERO): Infraestrutura Cognitiva (RRF + Knowledge Graph),
-- Financeira (Entitlements + Double-Entry Ledger + RTDN Idempotency) e
-- Fila Distribuída Anti-Zombie (SKIP LOCKED + Lease)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. FILA DISTRIBUÍDA ANTI-ZOMBIE (Módulo 1B) em public.orchestrator_tasks
-- ----------------------------------------------------------------------------
ALTER TABLE public.orchestrator_tasks
  ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS claimed_by TEXT,
  ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS max_attempts INT NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS last_error TEXT;

CREATE INDEX IF NOT EXISTS idx_orchestrator_tasks_claimable
  ON public.orchestrator_tasks (target, status, lease_expires_at, id)
  WHERE status IN ('PENDING', 'IN_PROGRESS');

CREATE OR REPLACE FUNCTION public.claim_next_orchestrator_task(
  p_target TEXT,
  p_worker_id TEXT,
  p_lease_seconds INT DEFAULT 300
)
RETURNS SETOF public.orchestrator_tasks
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_task_id INT;
BEGIN
  -- Move tarefas que excederam max_attempts após lease expirado para DEAD_LETTER
  UPDATE public.orchestrator_tasks
  SET status = 'DEAD_LETTER',
      last_error = COALESCE(last_error, 'Lease expirado acima de max_attempts'),
      updated_at = NOW()
  WHERE status = 'IN_PROGRESS'
    AND lease_expires_at IS NOT NULL
    AND lease_expires_at < NOW()
    AND attempt_count >= max_attempts;

  -- Seleciona próxima tarefa pendente ou com lease expirado usando SKIP LOCKED
  SELECT id INTO v_task_id
  FROM public.orchestrator_tasks
  WHERE (p_target IS NULL OR target = p_target)
    AND (
      status = 'PENDING'
      OR (status = 'IN_PROGRESS' AND lease_expires_at IS NOT NULL AND lease_expires_at < NOW() AND attempt_count < max_attempts)
    )
  ORDER BY id ASC
  FOR UPDATE SKIP LOCKED
  LIMIT 1;

  IF v_task_id IS NULL THEN
    RETURN;
  END IF;

  RETURN QUERY
  UPDATE public.orchestrator_tasks
  SET status = 'IN_PROGRESS',
      claimed_by = p_worker_id,
      lease_expires_at = NOW() + make_interval(secs => GREATEST(30, LEAST(p_lease_seconds, 3600))),
      attempt_count = attempt_count + 1,
      updated_at = NOW()
  WHERE id = v_task_id
  RETURNING *;
END;
$$;

-- ----------------------------------------------------------------------------
-- 2. FINANCEIRO GLOBAL, ENTITLEMENTS UNIFICADOS & RTDN (Módulos 1 & 2)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.entitlements (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  subject_key TEXT NOT NULL UNIQUE, -- ex.: purchase_token_hash ou user_id/email_hash
  source TEXT NOT NULL CHECK (source IN ('GOOGLE_PLAY', 'STRIPE', 'MOR_PADDLE', 'MOR_LEMON', 'PIX', 'CRYPTO_L2')),
  package_name TEXT NOT NULL DEFAULT 'com.rdf112.midiastore',
  product_id TEXT NOT NULL,
  product_type TEXT NOT NULL CHECK (product_type IN ('SUBS', 'INAPP', 'LIFETIME')),
  is_active BOOLEAN NOT NULL DEFAULT false,
  acknowledged BOOLEAN NOT NULL DEFAULT false,
  status TEXT NOT NULL DEFAULT 'UNKNOWN',
  expiry_time TIMESTAMPTZ,
  latest_order_id TEXT,
  last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_entitlements_active_expiry
  ON public.entitlements (is_active, expiry_time DESC)
  WHERE is_active = true;

CREATE TABLE IF NOT EXISTS public.financial_ledger (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  idempotency_key TEXT NOT NULL UNIQUE,
  event_type TEXT NOT NULL CHECK (
    event_type IN ('GROSS_SALE', 'STORE_FEE', 'TAX_PROVISION', 'REFUND', 'CHARGEBACK', 'NET_PROFIT_SPLIT_OWNER', 'NET_PROFIT_SPLIT_REINVEST')
  ),
  source TEXT NOT NULL CHECK (source IN ('GOOGLE_PLAY', 'STRIPE', 'MOR_PADDLE', 'MOR_LEMON', 'PIX', 'CRYPTO_L2')),
  order_id TEXT,
  product_id TEXT,
  direction TEXT NOT NULL CHECK (direction IN ('CREDIT', 'DEBIT')),
  amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
  currency CHAR(3) NOT NULL DEFAULT 'BRL',
  fx_rate_to_brl NUMERIC(14, 6) NOT NULL DEFAULT 1.000000,
  amount_brl_cents BIGINT NOT NULL CHECK (amount_brl_cents >= 0),
  is_test_transaction BOOLEAN NOT NULL DEFAULT false,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_financial_ledger_occurred
  ON public.financial_ledger (occurred_at DESC, is_test_transaction);

CREATE INDEX IF NOT EXISTS idx_financial_ledger_order_id
  ON public.financial_ledger (order_id)
  WHERE order_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.rtdn_events (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  message_id TEXT NOT NULL UNIQUE,
  notification_type INT,
  package_name TEXT NOT NULL,
  product_id TEXT,
  purchase_token_hash TEXT,
  event_time_millis BIGINT,
  processed BOOLEAN NOT NULL DEFAULT false,
  processing_result TEXT,
  raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rtdn_events_token_hash
  ON public.rtdn_events (purchase_token_hash, received_at DESC)
  WHERE purchase_token_hash IS NOT NULL;

ALTER TABLE public.entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.financial_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rtdn_events ENABLE ROW LEVEL SECURITY;

-- ----------------------------------------------------------------------------
-- 3. CÉREBRO HÍBRIDO (RRF BM25 + pgvector) & KNOWLEDGE GRAPH (Módulo 3)
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_memories_search_tsv_gin
  ON public.memories USING GIN (search_tsv);

CREATE OR REPLACE FUNCTION public.hybrid_memory_search(
  query_text TEXT,
  query_embedding vector(384),
  query_embedding_model TEXT DEFAULT 'gemini-embedding-001',
  match_count INT DEFAULT 8,
  rrf_k INT DEFAULT 60
)
RETURNS TABLE (
  id BIGINT,
  content TEXT,
  origem TEXT,
  categoria TEXT,
  confianca NUMERIC,
  versao INT,
  created_at TIMESTAMPTZ,
  embedding_model TEXT,
  fts_rank DOUBLE PRECISION,
  vec_sim DOUBLE PRECISION,
  rrf_score DOUBLE PRECISION
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, extensions
AS $$
  WITH q AS (
    SELECT
      COALESCE(
        NULLIF(
          to_tsquery(
            'portuguese',
            array_to_string(
              ARRAY(
                SELECT DISTINCT token
                FROM unnest(regexp_split_to_array(regexp_replace(lower(COALESCE(query_text, '')), '[^a-z0-9áéíóúâêôãõç]+', ' ', 'g'), '\s+')) AS token
                WHERE length(token) >= 3
                LIMIT 12
              ),
              ' | '
            )
          ),
          ''::tsquery
        ),
        plainto_tsquery('portuguese', COALESCE(query_text, ''))
      ) AS tsq
  ),
  fts_candidates AS (
    SELECT
      m.id,
      ts_rank_cd(m.search_tsv, q.tsq)::DOUBLE PRECISION AS fts_rank,
      ROW_NUMBER() OVER (
        ORDER BY ts_rank_cd(m.search_tsv, q.tsq) DESC, m.id DESC
      ) AS fts_pos
    FROM public.memories m, q
    WHERE m.is_active = true
      AND m.content !~ '\[Ciclo #[0-9]+\]'
      AND m.search_tsv @@ q.tsq
    LIMIT GREATEST(match_count * 4, 32)
  ),
  vec_candidates AS (
    SELECT
      m.id,
      (1.0 - (m.embedding <=> query_embedding))::DOUBLE PRECISION AS vec_sim,
      ROW_NUMBER() OVER (
        ORDER BY (m.embedding <=> query_embedding) ASC, m.id DESC
      ) AS vec_pos
    FROM public.memories m
    WHERE m.is_active = true
      AND m.content !~ '\[Ciclo #[0-9]+\]'
      AND m.embedding IS NOT NULL
      AND m.embedding_model = query_embedding_model
    LIMIT GREATEST(match_count * 4, 32)
  ),
  fused AS (
    SELECT
      COALESCE(f.id, v.id) AS id,
      COALESCE(f.fts_rank, 0.0) AS fts_rank,
      COALESCE(v.vec_sim, 0.0) AS vec_sim,
      (
        COALESCE(1.0 / (rrf_k + f.fts_pos), 0.0) +
        COALESCE(1.0 / (rrf_k + v.vec_pos), 0.0)
      )::DOUBLE PRECISION AS rrf_score
    FROM fts_candidates f
    FULL OUTER JOIN vec_candidates v ON f.id = v.id
  )
  SELECT
    m.id,
    m.content,
    m.origem,
    m.categoria,
    m.confianca,
    m.versao,
    m.created_at,
    m.embedding_model,
    u.fts_rank,
    u.vec_sim,
    (u.rrf_score + (COALESCE(m.confianca::DOUBLE PRECISION, 0.85) * 0.002)) AS rrf_score
  FROM fused u
  JOIN public.memories m ON m.id = u.id
  ORDER BY (u.rrf_score + (COALESCE(m.confianca::DOUBLE PRECISION, 0.85) * 0.002)) DESC, m.id DESC
  LIMIT GREATEST(1, LEAST(match_count, 30));
$$;

CREATE TABLE IF NOT EXISTS public.kg_nodes (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  node_key TEXT NOT NULL UNIQUE, -- ex.: 'entity:verify-play-purchase', 'concept:rrf', 'goal:r1m'
  label TEXT NOT NULL,
  node_type TEXT NOT NULL CHECK (node_type IN ('ENTITY', 'CONCEPT', 'RULE', 'GOAL', 'FILE', 'BUG', 'MILESTONE', 'SYSTEM')),
  summary TEXT,
  salience NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
  access_count INT NOT NULL DEFAULT 1,
  last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kg_nodes_type_salience
  ON public.kg_nodes (node_type, salience DESC);

CREATE TABLE IF NOT EXISTS public.kg_edges (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source_node_id BIGINT NOT NULL REFERENCES public.kg_nodes(id) ON DELETE CASCADE,
  target_node_id BIGINT NOT NULL REFERENCES public.kg_nodes(id) ON DELETE CASCADE,
  relation TEXT NOT NULL CHECK (relation IN ('CAUSES', 'BLOCKS', 'DEPENDS_ON', 'UNLOCKS', 'IMPLEMENTS', 'SUPERSEDES', 'PART_OF', 'RELATED_TO')),
  weight NUMERIC(5, 4) NOT NULL DEFAULT 0.7000,
  source_memory_id BIGINT REFERENCES public.memories(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (source_node_id, target_node_id, relation)
);

CREATE INDEX IF NOT EXISTS idx_kg_edges_source ON public.kg_edges (source_node_id, weight DESC);
CREATE INDEX IF NOT EXISTS idx_kg_edges_target ON public.kg_edges (target_node_id, weight DESC);

ALTER TABLE public.kg_nodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.kg_edges ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.kg_traverse_2hop(
  seed_keys TEXT[],
  max_results INT DEFAULT 12
)
RETURNS TABLE (
  hop INT,
  source_key TEXT,
  source_label TEXT,
  relation TEXT,
  target_key TEXT,
  target_label TEXT,
  target_type TEXT,
  weight NUMERIC
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  WITH RECURSIVE walk AS (
    SELECT
      1 AS hop,
      s.id AS src_id,
      s.node_key AS source_key,
      s.label AS source_label,
      e.relation,
      t.id AS tgt_id,
      t.node_key AS target_key,
      t.label AS target_label,
      t.node_type AS target_type,
      e.weight
    FROM public.kg_nodes s
    JOIN public.kg_edges e ON e.source_node_id = s.id
    JOIN public.kg_nodes t ON t.id = e.target_node_id
    WHERE s.node_key = ANY(seed_keys)

    UNION ALL

    SELECT
      w.hop + 1,
      s2.id,
      s2.node_key,
      s2.label,
      e2.relation,
      t2.id,
      t2.node_key,
      t2.label,
      t2.node_type,
      (w.weight * e2.weight)::NUMERIC(5,4)
    FROM walk w
    JOIN public.kg_nodes s2 ON s2.id = w.tgt_id
    JOIN public.kg_edges e2 ON e2.source_node_id = s2.id
    JOIN public.kg_nodes t2 ON t2.id = e2.target_node_id
    WHERE w.hop < 2
      AND t2.node_key <> ALL(seed_keys)
  )
  SELECT DISTINCT ON (source_key, relation, target_key)
    hop, source_key, source_label, relation, target_key, target_label, target_type, weight
  FROM walk
  ORDER BY source_key, relation, target_key, weight DESC
  LIMIT GREATEST(1, LEAST(max_results, 30));
$$;
