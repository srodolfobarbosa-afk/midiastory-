import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

async function runSql(query) {
  const token = getSupabaseToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`SQL HTTP ${res.status}: ${text}`);
  return JSON.parse(text);
}

async function main() {
  const [stats] = await runSql("SELECT public.get_prelaunch_stats() AS stats;");
  const bySource = await runSql(`
    SELECT
      COALESCE(utm_source, '(direct)') AS source,
      COUNT(*) FILTER (WHERE event_type = 'page_view') AS views,
      COUNT(*) FILTER (WHERE event_type = 'cta_click') AS cta_clicks,
      COUNT(*) FILTER (WHERE event_type = 'share_click') AS share_clicks
    FROM public.web_page_events
    GROUP BY 1
    ORDER BY views DESC, source ASC
    LIMIT 12;
  `);
  const byLocale = await runSql(`
    SELECT
      locale,
      COUNT(*) FILTER (WHERE event_type = 'page_view') AS views,
      COUNT(DISTINCT visitor_id) FILTER (WHERE event_type = 'page_view') AS unique_visitors
    FROM public.web_page_events
    GROUP BY 1
    ORDER BY views DESC, locale ASC
    LIMIT 10;
  `);
  const waitlistBySource = await runSql(`
    SELECT COALESCE(source, 'unknown') AS source, COUNT(*) AS signups
    FROM public.beta_testers
    GROUP BY 1
    ORDER BY signups DESC, source ASC;
  `);

  console.log("=== MIDIASTORE PRE-LAUNCH PULSE REPORT ===");
  console.log("Resumo geral:", JSON.stringify(stats?.stats || {}, null, 2));
  console.log("Trafego por utm_source:", JSON.stringify(bySource, null, 2));
  console.log("Trafego por idioma:", JSON.stringify(byLocale, null, 2));
  console.log("Fila de espera / Testadores por origem:", JSON.stringify(waitlistBySource, null, 2));
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
