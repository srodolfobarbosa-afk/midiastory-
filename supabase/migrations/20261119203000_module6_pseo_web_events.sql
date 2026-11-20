-- ============================================================================
-- MÓDULO 6: RASTREAMENTO REAL DE VISITAS & CLIQUES NAS PÁGINAS pSEO
-- Expande public.product_events para aceitar page_view e play_store_click
-- + View agregada public.v_pseo_traffic_report
-- ============================================================================

ALTER TABLE public.product_events
  DROP CONSTRAINT IF EXISTS product_events_event_name_check;

ALTER TABLE public.product_events
  ADD CONSTRAINT product_events_event_name_check CHECK (
    event_name IN (
      'install_first_open',
      'first_scan_completed',
      'paywall_viewed',
      'purchase_started',
      'purchase_completed',
      'cleanup_completed',
      'retention_ping',
      'page_view',
      'play_store_click'
    )
  );

CREATE OR REPLACE VIEW public.v_pseo_traffic_report
WITH (security_invoker = true) AS
SELECT
  COALESCE(properties->>'path', 'unknown') AS page_path,
  COALESCE(locale, 'unknown') AS lang,
  COUNT(*) FILTER (WHERE event_name = 'page_view') AS page_views,
  COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'page_view') AS unique_visitors,
  COUNT(*) FILTER (WHERE event_name = 'play_store_click') AS play_store_clicks,
  CASE
    WHEN COUNT(*) FILTER (WHERE event_name = 'page_view') > 0
    THEN ROUND(
      (COUNT(*) FILTER (WHERE event_name = 'play_store_click')::NUMERIC /
       COUNT(*) FILTER (WHERE event_name = 'page_view')) * 100, 2
    )
    ELSE 0
  END AS ctr_pct,
  MAX(created_at) AS last_event_at
FROM public.product_events
WHERE event_name IN ('page_view', 'play_store_click')
GROUP BY COALESCE(properties->>'path', 'unknown'), COALESCE(locale, 'unknown')
ORDER BY play_store_clicks DESC, page_views DESC;
