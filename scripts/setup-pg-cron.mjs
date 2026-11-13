import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const token = getSupabaseToken();
const projectRef = "usmqtnlsaavxqguwrtix";

const sql = `
DO $$
BEGIN
  -- Remove job antigo se já existir
  PERFORM cron.unschedule('orchestrator-24-7-heartbeat')
  FROM cron.job
  WHERE jobname = 'orchestrator-24-7-heartbeat';
EXCEPTION
  WHEN OTHERS THEN NULL;
END
$$;

SELECT cron.schedule(
  'orchestrator-24-7-heartbeat',
  '*/20 * * * *',
  $cron$
  SELECT net.http_post(
    url := 'https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/native-orchestrator',
    headers := '{"Content-Type": "application/json"}'::jsonb,
    body := '{"action": "RUN_LOOP"}'::jsonb
  );
  $cron$
);
`;

async function main() {
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query: sql }),
  });

  const data = await res.json();
  console.log("pg_cron configurado com sucesso no Supabase:", data);
}

main().catch(console.error);
