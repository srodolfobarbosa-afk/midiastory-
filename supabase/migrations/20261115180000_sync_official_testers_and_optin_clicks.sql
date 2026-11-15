BEGIN;

CREATE TABLE IF NOT EXISTS public.play_console_sync_state (
  id integer PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  official_optin_testers integer NOT NULL DEFAULT 12 CHECK (official_optin_testers >= 0),
  closed_test_day integer NOT NULL DEFAULT 2 CHECK (closed_test_day >= 0),
  closed_test_target_days integer NOT NULL DEFAULT 14 CHECK (closed_test_target_days >= 1),
  notes text,
  synced_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.play_console_sync_state (id, official_optin_testers, closed_test_day, closed_test_target_days, notes)
VALUES (1, 12, 2, 14, 'Sincronizado com Play Console em 2026-11-15: 12 testadores oficiais confirmados no Dia 2 de 14.')
ON CONFLICT (id) DO UPDATE SET
  official_optin_testers = GREATEST(public.play_console_sync_state.official_optin_testers, EXCLUDED.official_optin_testers),
  closed_test_day = GREATEST(public.play_console_sync_state.closed_test_day, EXCLUDED.closed_test_day),
  notes = EXCLUDED.notes,
  synced_at = now();

ALTER TABLE public.play_console_sync_state ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.play_console_sync_state FROM PUBLIC, anon, authenticated;
GRANT ALL ON TABLE public.play_console_sync_state TO service_role;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'play_console_sync_state' AND policyname = 'service_role total play_console_sync_state'
  ) THEN
    CREATE POLICY "service_role total play_console_sync_state"
      ON public.play_console_sync_state
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
  WITH counts AS (
    SELECT
      COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'page_view'), 0) AS total_visits,
      COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'page_view' AND created_at >= now() - INTERVAL '24 hours'), 0) AS visits_24h,
      COALESCE((SELECT COUNT(DISTINCT visitor_id)::bigint FROM public.web_page_events WHERE event_type = 'page_view' AND visitor_id IS NOT NULL), 0) AS unique_visitors,
      COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'share_click'), 0) AS share_clicks,
      COALESCE((SELECT COUNT(*)::bigint FROM public.web_page_events WHERE event_type = 'cta_click'), 0) AS cta_clicks,
      COALESCE((
        SELECT COUNT(*)::bigint
        FROM public.web_page_events
        WHERE event_type = 'optin_click'
           OR (event_type = 'cta_click' AND utm_source IN ('hero_optin', 'step2_optin'))
      ), 0) AS optin_clicks,
      COALESCE((SELECT COUNT(DISTINCT lower(trim(email)))::bigint FROM public.beta_testers), 0) AS waitlist_count,
      COALESCE((SELECT official_optin_testers::bigint FROM public.play_console_sync_state WHERE id = 1), 12) AS official_testers_count,
      COALESCE((SELECT closed_test_day::integer FROM public.play_console_sync_state WHERE id = 1), 2) AS closed_test_day,
      COALESCE((SELECT closed_test_target_days::integer FROM public.play_console_sync_state WHERE id = 1), 14) AS closed_test_target_days
  )
  SELECT jsonb_build_object(
    'total_visits', total_visits,
    'visits_24h', visits_24h,
    'unique_visitors', unique_visitors,
    'share_clicks', share_clicks,
    'cta_clicks', cta_clicks,
    'optin_clicks', optin_clicks,
    'waitlist_count', waitlist_count,
    'official_testers_count', official_testers_count,
    'active_testers_display', GREATEST(official_testers_count, waitlist_count + optin_clicks),
    'closed_test_day', closed_test_day,
    'closed_test_target_days', closed_test_target_days,
    'updated_at', now()
  )
  FROM counts;
$$;

REVOKE ALL ON FUNCTION public.get_prelaunch_stats() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_prelaunch_stats() TO anon, authenticated, service_role;

COMMIT;
