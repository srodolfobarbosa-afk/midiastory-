-- ============================================================================
-- MÓDULO 7: MOTOR DE VENDAS CIENTÍFICAS & APRENDIZADO AUTÔNOMO (THOMPSON SAMPLING)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.sales_experiments (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  experiment_key TEXT NOT NULL,
  variant_key TEXT NOT NULL,
  locale TEXT NOT NULL DEFAULT 'pt-BR',
  headline TEXT NOT NULL,
  subheadline TEXT NOT NULL,
  highlighted_sku TEXT NOT NULL CHECK (highlighted_sku IN ('sweep_pro_monthly', 'sweep_pro_annual', 'sweep_pro_lifetime')),
  cta_label TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'WINNER', 'PAUSED')),
  impressions BIGINT NOT NULL DEFAULT 0,
  clicks BIGINT NOT NULL DEFAULT 0,
  purchases BIGINT NOT NULL DEFAULT 0,
  refunds BIGINT NOT NULL DEFAULT 0,
  alpha NUMERIC(12, 4) NOT NULL DEFAULT 1.0000,
  beta NUMERIC(12, 4) NOT NULL DEFAULT 1.0000,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (experiment_key, variant_key, locale)
);

CREATE INDEX IF NOT EXISTS idx_sales_experiments_active
  ON public.sales_experiments (experiment_key, locale, status)
  WHERE status IN ('ACTIVE', 'WINNER');

ALTER TABLE public.sales_experiments ENABLE ROW LEVEL SECURITY;

-- Seed idempotente: 3 variantes éticas e verdadeiras para o Paywall Pro (PT-BR e EN-US)
INSERT INTO public.sales_experiments (
  experiment_key, variant_key, locale, headline, subheadline, highlighted_sku, cta_label, status
) VALUES
  (
    'paywall_v1', 'A_speed_space', 'pt-BR',
    'Libere gigabytes agora sem perder nenhuma foto',
    'Duplicatas exatas SHA-256 e vídeos gigantes em 1 toque. Cancele quando quiser na Google Play.',
    'sweep_pro_annual',
    'Desbloquear Pro Anual (Economize 55%)',
    'ACTIVE'
  ),
  (
    'paywall_v1', 'B_privacy_light', 'pt-BR',
    '5,9 MB • 100% no seu aparelho • Zero anúncios',
    'Privacidade total sem nuvem e sem exclusão automática. Revisão humana em cada limpeza.',
    'sweep_pro_annual',
    'Ativar Midiastore Pro Agora',
    'ACTIVE'
  ),
  (
    'paywall_v1', 'C_lifetime_anchor', 'pt-BR',
    'Pague uma vez ou assine por menos de R$ 6,70/mês',
    'Escolha o plano anual com 55% OFF ou garanta acesso Vitalício sem mensalidades.',
    'sweep_pro_lifetime',
    'Ver Planos Pro (Anual ou Vitalício)',
    'ACTIVE'
  ),
  (
    'paywall_v1', 'A_speed_space', 'en-US',
    'Reclaim gigabytes safely without losing a single memory',
    'Exact SHA-256 duplicates & large videos in seconds. Cancel anytime on Google Play.',
    'sweep_pro_annual',
    'Unlock Pro Annual (Save 55%)',
    'ACTIVE'
  ),
  (
    'paywall_v1', 'B_privacy_light', 'en-US',
    '5.9 MB • 100% On-Device • Zero Bloatware',
    'Your photos never leave your phone. Official Android confirmation on every cleanup.',
    'sweep_pro_annual',
    'Upgrade to Midiastore Pro',
    'ACTIVE'
  ),
  (
    'paywall_v1', 'C_lifetime_anchor', 'en-US',
    'Pay once forever or save 55% with Pro Annual',
    'Full-library SHA-256 duplicate cleanup with zero recurring fees on Lifetime.',
    'sweep_pro_lifetime',
    'Choose Annual or Lifetime Pro',
    'ACTIVE'
  )
ON CONFLICT (experiment_key, variant_key, locale) DO NOTHING;
