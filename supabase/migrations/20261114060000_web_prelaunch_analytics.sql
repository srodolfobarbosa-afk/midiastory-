-- =============================================================================
-- Pre-launch Web Analytics & Public Waitlist Counter (Strict RLS, Zero PII Leak)
-- Target Project: usmqtnlsaavxqguwrtix (Midiastore)
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.web_page_events (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  page_path TEXT NOT NULL CHECK (char_length(page_path) BETWEEN 1 AND 200),
  locale TEXT NOT NULL DEFAULT 'pt-BR' CHECK (char_length(locale) BETWEEN 2 AND 16),
  event_type TEXT NOT NULL DEFAULT 'page_view' CHECK (event_type IN ('page_view', 'share_click', 'cta_click', 'waitlist_submit')),
  utm_source TEXT CHECK (utm_source IS NULL OR char_length(utm_source) <= 80),
  utm_medium TEXT CHECK (utm_medium IS NULL OR char_length(utm_medium) <= 80),
  utm_campaign TEXT CHECK (utm_campaign IS NULL OR char_length(utm_campaign) <= 100),
  referrer_host TEXT CHECK (referrer_host IS NULL OR char_length(referrer_host) <= 120),
  visitor_id TEXT CHECK (visitor_id IS NULL OR char_length(visitor_id) <= 64)
);

CREATE INDEX IF NOT EXISTS idx_web_page_events_created_at
  ON public.web_page_events (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_web_page_events_type_created
  ON public.web_page_events (event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_web_page_events_utm_source
  ON public.web_page_events (utm_source)
  WHERE utm_source IS NOT NULL;

ALTER TABLE public.web_page_events ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.web_page_events FROM PUBLIC, anon, authenticated;
GRANT INSERT ON TABLE public.web_page_events TO anon, authenticated;
GRANT ALL ON TABLE public.web_page_events TO service_role;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'web_page_events' AND policyname = 'anon insert web_page_events'
  ) THEN
    CREATE POLICY "anon insert web_page_events"
      ON public.web_page_events
      FOR INSERT
      TO anon, authenticated
      WITH CHECK (
        char_length(page_path) BETWEEN 1 AND 200
        AND char_length(locale) BETWEEN 2 AND 16
        AND event_type IN ('page_view', 'share_click', 'cta_click', 'waitlist_submit')
      );
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'web_page_events' AND policyname = 'service_role total web_page_events'
  ) THEN
    CREATE POLICY "service_role total web_page_events"
      ON public.web_page_events
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;
END
$$;

CREATE OR REPLACE FUNCTION public.get_prelaunch_stats()
RETURNS jsonb
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT jsonb_build_object(
    'total_visits', COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'page_view'), 0),
    'visits_24h', COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'page_view' AND created_at >= now() - INTERVAL '24 hours'), 0),
    'unique_visitors', COALESCE((SELECT COUNT(DISTINCT visitor_id)::bigint FROM public.web_page_events WHERE event_type = 'page_view' AND visitor_id IS NOT NULL), 0),
    'share_clicks', COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'share_click'), 0),
    'cta_clicks', COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'cta_click'), 0),
    'waitlist_count', COALESCE((SELECT COUNT(DISTINCT lower(trim(email)))::bigint FROM public.beta_testers), 0),
    'updated_at', now()
  );
$$;

REVOKE ALL ON FUNCTION public.get_prelaunch_stats() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_prelaunch_stats() TO anon, authenticated, service_role;

COMMIT;
