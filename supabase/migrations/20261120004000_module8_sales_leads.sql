-- ============================================================================
-- MÓDULO 8: RADAR DE DOR EM TEMPO REAL (public.sales_leads + RLS)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.sales_leads (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source TEXT NOT NULL, -- ex.: 'reddit:r/samsung', 'reddit:r/Android'
  post_url TEXT NOT NULL UNIQUE,
  title TEXT NOT NULL,
  detected_pain TEXT NOT NULL,
  locale TEXT NOT NULL DEFAULT 'en-US',
  suggested_guide_url TEXT NOT NULL,
  draft_reply TEXT NOT NULL,
  post_created_at TIMESTAMPTZ,
  status TEXT NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'REVIEWED', 'REPLIED', 'IGNORED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sales_leads_status_created
  ON public.sales_leads (status, created_at DESC);

ALTER TABLE public.sales_leads ENABLE ROW LEVEL SECURITY;
