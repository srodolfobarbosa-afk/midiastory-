#!/usr/bin/env node
/**
 * MONITOR DE TELEMETRIA AO VIVO DO SITE (Midiastore)
 *
 * Consulta o Supabase em tempo real para obter métricas reais do funil web:
 * - Visitas totais (`total_visits`), visitantes únicos (`unique_visitors`) e 24h (`visits_24h`)
 * - Cliques diretos no convite oficial Google Play (`optin_clicks`) e compartilhamentos (`share_clicks`)
 * - Inscritos na fila (`waitlist_count`), base oficial sincronizada da Play Console (`official_testers_count`)
 *   e contador exibido no site (`active_testers_display`)
 * - Dias do teste fechado (`closed_test_day / closed_test_target_days`)
 *
 * Uso:
 *   node scripts/monitor-site-telemetry.mjs
 *   node scripts/monitor-site-telemetry.mjs --json
 */

import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const SUPABASE_URL = `https://${SUPABASE_PROJECT_REF}.supabase.co`;
const ANON_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

export async function fetchLiveSiteTelemetry() {
  const rpcRes = await fetch(`${SUPABASE_URL}/rest/v1/rpc/get_prelaunch_stats`, {
    method: "POST",
    headers: {
      apikey: ANON_KEY,
      Authorization: `Bearer ${ANON_KEY}`,
      "Content-Type": "application/json",
    },
    body: "{}",
    signal: AbortSignal.timeout(15000),
  });

  if (!rpcRes.ok) {
    const body = await rpcRes.text();
    throw new Error(`Falha ao consultar get_prelaunch_stats (HTTP ${rpcRes.status}): ${body}`);
  }

  const stats = await rpcRes.json();

  let recentSignups24h = null;
  let recentEmails = [];
  try {
    const token = getSupabaseToken();
    const qRes = await fetch(
      `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          query:
            "SELECT COUNT(*)::int AS count_24h FROM public.beta_testers WHERE created_at >= now() - INTERVAL '24 hours';",
        }),
        signal: AbortSignal.timeout(15000),
      }
    );
    if (qRes.ok) {
      const rows = await qRes.json();
      recentSignups24h = rows?.[0]?.count_24h ?? null;
    }

    const listRes = await fetch(
      `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          query:
            "SELECT id, email, created_at FROM public.beta_testers ORDER BY created_at DESC LIMIT 20;",
        }),
        signal: AbortSignal.timeout(15000),
      }
    );
    if (listRes.ok) {
      recentEmails = await listRes.json();
    }
  } catch {
    // Fallback silencioso quando rodando sem token de gerenciamento (ex.: apenas anon key)
  }

  return {
    ...stats,
    recent_signups_24h: recentSignups24h,
    recent_emails: recentEmails,
    checked_at: new Date().toISOString(),
  };
}

async function main() {
  const jsonOnly = process.argv.includes("--json");
  const data = await fetchLiveSiteTelemetry();

  if (jsonOnly) {
    console.log(JSON.stringify(data, null, 2));
    return;
  }

  console.log("=== TELEMETRIA AO VIVO — MIDIASTORE (SUPABASE) ===");
  console.log(`👁️  Visitas totais:              ${data.total_visits ?? 0}`);
  console.log(`🧭 Visitantes únicos:           ${data.unique_visitors ?? 0}`);
  console.log(`🔥 Visitas (últimas 24h):       ${data.visits_24h ?? 0}`);
  console.log(`📲 Cliques no Convite Play:     ${data.optin_clicks ?? 0}`);
  console.log(`📝 Inscritos no formulário:     ${data.waitlist_count ?? 0}`);
  if (data.recent_signups_24h !== null) {
    console.log(`🆕 Novos cadastros (24h):       ${data.recent_signups_24h}`);
  }
  console.log(`✅ Testadores oficiais (Play):  ${data.official_testers_count ?? "N/D"}`);
  console.log(`👥 Exibido no site (sincron.):  ${data.active_testers_display ?? data.waitlist_count ?? 0}`);
  if (data.closed_test_day != null && data.closed_test_target_days != null) {
    console.log(`⏳ Relógio 14 dias:             Dia ${data.closed_test_day} de ${data.closed_test_target_days}`);
  }
  if (Array.isArray(data.recent_emails) && data.recent_emails.length > 0) {
    console.log("📧 E-mails em public.beta_testers:");
    for (const row of data.recent_emails) {
      console.log(`   - ${row.email} (${row.created_at})`);
    }
  }
  console.log(`🕒 Atualizado em:               ${data.updated_at || data.checked_at}`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((err) => {
    console.error("[monitor-site-telemetry] Erro:", err.message);
    process.exit(1);
  });
}
