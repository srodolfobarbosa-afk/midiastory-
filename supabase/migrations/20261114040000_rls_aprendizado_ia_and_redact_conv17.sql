-- =============================================================================
-- Safe Migration Proposal: RLS Hardening (public.aprendizado_ia & internal tables),
-- pg_cron PAUSED Noise Guard, and Historical Secret Redaction (conversations #17)
-- Target Project: usmqtnlsaavxqguwrtix (Midiastore)
-- NOTE: Prepared for review/sign-off. Not auto-executed against live database.
-- =============================================================================

BEGIN;

-- 1. Enable Row Level Security on public.aprendizado_ia (currently rowsecurity = false)
ALTER TABLE public.aprendizado_ia ENABLE ROW LEVEL SECURITY;

-- 2. Revoke direct Data API (anon / authenticated) access from internal backend/audit tables
REVOKE ALL ON TABLE public.aprendizado_ia FROM anon, authenticated;
REVOKE ALL ON TABLE public.play_purchases FROM anon, authenticated;
REVOKE ALL ON TABLE public.orchestrator_state FROM anon, authenticated;
REVOKE ALL ON TABLE public.orchestrator_tasks FROM anon, authenticated;
REVOKE ALL ON TABLE public.orchestrator_logs FROM anon, authenticated;

-- 3. Ensure explicit service_role policies exist on all internal tables
--    (Matches existing convention on conversations, documents, and memories)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'aprendizado_ia' AND policyname = 'service_role total aprendizado_ia'
  ) THEN
    CREATE POLICY "service_role total aprendizado_ia"
      ON public.aprendizado_ia
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'play_purchases' AND policyname = 'service_role total play_purchases'
  ) THEN
    CREATE POLICY "service_role total play_purchases"
      ON public.play_purchases
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'orchestrator_state' AND policyname = 'service_role total orchestrator_state'
  ) THEN
    CREATE POLICY "service_role total orchestrator_state"
      ON public.orchestrator_state
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'orchestrator_tasks' AND policyname = 'service_role total orchestrator_tasks'
  ) THEN
    CREATE POLICY "service_role total orchestrator_tasks"
      ON public.orchestrator_tasks
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'orchestrator_logs' AND policyname = 'service_role total orchestrator_logs'
  ) THEN
    CREATE POLICY "service_role total orchestrator_logs"
      ON public.orchestrator_logs
      FOR ALL
      TO service_role
      USING (true)
      WITH CHECK (true);
  END IF;
END
$$;

-- 4. Guard pg_cron heartbeat so it does not fire HTTP POSTs when orchestrator_state is PAUSED or META_CONCLUIDA
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'cron') THEN
    PERFORM cron.unschedule('orchestrator-24-7-heartbeat')
    FROM cron.job
    WHERE jobname = 'orchestrator-24-7-heartbeat';

    PERFORM cron.schedule(
      'orchestrator-24-7-heartbeat',
      '*/20 * * * *',
      $cron$
      SELECT net.http_post(
        url := 'https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/native-orchestrator',
        headers := '{"Content-Type": "application/json"}'::jsonb,
        body := '{"action": "RUN_LOOP"}'::jsonb
      )
      FROM (
        SELECT status, step_count, max_steps
        FROM public.orchestrator_state
        ORDER BY id DESC
        LIMIT 1
      ) s
      WHERE s.status = 'RUNNING'
        AND s.step_count < s.max_steps;
      $cron$
    );
  END IF;
END
$$;

-- 5. Redact historical Google AI Studio key in public.conversations #17 (REQUIRES COORDINATOR SIGN-OFF)
--    Preserves the conversation record and non-sensitive text while scrubbing the AQ.* token prefix.
UPDATE public.conversations
SET
  question = regexp_replace(
    regexp_replace(
      COALESCE(question, ''),
      'AQ\.[A-Za-z0-9_.-]+',
      '[REDACTED_GOOGLE_AQ_KEY]',
      'g'
    ),
    '(sbp_[A-Za-z0-9_]+|sk-or-v1-[A-Za-z0-9]+|AIza[A-Za-z0-9_-]+|ghp_[A-Za-z0-9]+)',
    '[REDACTED_SECRET]',
    'g'
  ),
  answer = regexp_replace(
    regexp_replace(
      COALESCE(answer, ''),
      'AQ\.[A-Za-z0-9_.-]+',
      '[REDACTED_GOOGLE_AQ_KEY]',
      'g'
    ),
    '(sbp_[A-Za-z0-9_]+|sk-or-v1-[A-Za-z0-9]+|AIza[A-Za-z0-9_-]+|ghp_[A-Za-z0-9]+)',
    '[REDACTED_SECRET]',
    'g'
  )
WHERE id = 17
  AND (
    COALESCE(question, '') ~ '(AQ\.[A-Za-z0-9_.-]+|sbp_|sk-or-v1-|AIza|ghp_)'
    OR COALESCE(answer, '') ~ '(AQ\.[A-Za-z0-9_.-]+|sbp_|sk-or-v1-|AIza|ghp_)'
  );

COMMIT;
