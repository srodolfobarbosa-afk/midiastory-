-- ============================================================================
-- MÓDULO 4 (APPSEC, GUARDRAILS GOODHART & GROWTH/UNIT ECONOMICS)
-- Migration 100% idempotente com plano de rollback não-destrutivo documentado.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. IMUTABILIDADE APPEND-ONLY EM public.financial_ledger (AppSec Guardrail)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.prevent_financial_ledger_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RAISE EXCEPTION 'APPSEC_GUARDRAIL: public.financial_ledger is strictly append-only (% blocked on id=%). Insert a compensating entry instead.', TG_OP, OLD.id;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_financial_ledger_mutation ON public.financial_ledger;
CREATE TRIGGER trg_prevent_financial_ledger_mutation
  BEFORE UPDATE OR DELETE ON public.financial_ledger
  FOR EACH ROW
  EXECUTE FUNCTION public.prevent_financial_ledger_mutation();

-- ----------------------------------------------------------------------------
-- 2. GUARDRAIL GOODHART EM public.memories (Anti-Exclusão de Memórias Críticas)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.prevent_critical_memory_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF OLD.categoria IN ('strategic', 'correction', 'preference') OR COALESCE(OLD.confianca, 0) >= 0.90 THEN
    RAISE EXCEPTION 'GOODHART_GUARDRAIL: physical DELETE blocked on critical memory id=% (categoria=%, confianca=%). Use is_active=false + superseded_by instead.', OLD.id, OLD.categoria, OLD.confianca;
  END IF;
  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_critical_memory_delete ON public.memories;
CREATE TRIGGER trg_prevent_critical_memory_delete
  BEFORE DELETE ON public.memories
  FOR EACH ROW
  EXECUTE FUNCTION public.prevent_critical_memory_delete();

-- ----------------------------------------------------------------------------
-- 3. UNIT ECONOMICS & COORTES ANÔNIMAS (Módulo 4B — Zero PII, LGPD Compliant)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.product_events (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  install_hash TEXT NOT NULL, -- SHA-256 salted anônimo da instalação (zero PII)
  event_name TEXT NOT NULL CHECK (
    event_name IN (
      'install_first_open',
      'first_scan_completed',
      'paywall_viewed',
      'purchase_started',
      'purchase_completed',
      'cleanup_completed',
      'retention_ping'
    )
  ),
  app_version TEXT,
  locale TEXT,
  cohort_day DATE NOT NULL DEFAULT CURRENT_DATE,
  properties JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_events_name_created
  ON public.product_events (event_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_events_cohort_install
  ON public.product_events (cohort_day DESC, install_hash, event_name);

ALTER TABLE public.product_events ENABLE ROW LEVEL SECURITY;

-- View de Funil de Conversão & Unit Economics (últimos 30 dias)
CREATE OR REPLACE VIEW public.v_funnel_unit_economics
WITH (security_invoker = true) AS
SELECT
  cohort_day,
  COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'install_first_open') AS installs,
  COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'first_scan_completed') AS first_scans,
  COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'paywall_viewed') AS paywall_views,
  COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'purchase_completed') AS purchases,
  CASE
    WHEN COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'install_first_open') > 0
    THEN ROUND(
      (COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'first_scan_completed')::NUMERIC /
       COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'install_first_open')) * 100, 2
    )
    ELSE 0
  END AS scan_activation_pct,
  CASE
    WHEN COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'paywall_viewed') > 0
    THEN ROUND(
      (COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'purchase_completed')::NUMERIC /
       COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'paywall_viewed')) * 100, 2
    )
    ELSE 0
  END AS paywall_conversion_pct
FROM public.product_events
WHERE cohort_day >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY cohort_day
ORDER BY cohort_day DESC;

-- ============================================================================
-- PLANO DE ROLLBACK SEGURO (NÃO-DESTRUTIVO — SOMENTE REMOVER TRIGGERS/VIEW):
--   DROP TRIGGER IF EXISTS trg_prevent_financial_ledger_mutation ON public.financial_ledger;
--   DROP FUNCTION IF EXISTS public.prevent_financial_ledger_mutation();
--   DROP TRIGGER IF EXISTS trg_prevent_critical_memory_delete ON public.memories;
--   DROP FUNCTION IF EXISTS public.prevent_critical_memory_delete();
--   DROP VIEW IF EXISTS public.v_funnel_unit_economics;
--   (Nunca executar DROP TABLE em produção sem backup verificado.)
-- ============================================================================
