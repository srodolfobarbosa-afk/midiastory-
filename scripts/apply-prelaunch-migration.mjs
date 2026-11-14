import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const SUPABASE_URL = `https://${SUPABASE_PROJECT_REF}.supabase.co`;
const SUPABASE_ANON_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

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
  const migrationPath = path.join(projectRoot, "supabase", "migrations", "20261114060000_web_prelaunch_analytics.sql");
  const sql = fs.readFileSync(migrationPath, "utf8");
  console.log("Aplicando migracao 20261114060000_web_prelaunch_analytics.sql...");
  await runSql(sql);
  await runSql("NOTIFY pgrst, 'reload schema';");

  // Espera breve para o schema cache do PostgREST recarregar
  await new Promise((r) => setTimeout(r, 1500));

  const rlsCheck = await runSql(
    "SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'web_page_events';"
  );
  console.log("RLS web_page_events:", JSON.stringify(rlsCheck));

  // Testa vazamento zero de SELECT anon
  const anonReadEvents = await fetch(`${SUPABASE_URL}/rest/v1/web_page_events?select=*&limit=1`, {
    headers: { apikey: SUPABASE_ANON_KEY, Authorization: `Bearer ${SUPABASE_ANON_KEY}` },
  });
  console.log("Anon SELECT web_page_events status (esperado 401/403):", anonReadEvents.status);

  const anonReadTesters = await fetch(`${SUPABASE_URL}/rest/v1/beta_testers?select=email&limit=1`, {
    headers: { apikey: SUPABASE_ANON_KEY, Authorization: `Bearer ${SUPABASE_ANON_KEY}` },
  });
  console.log("Anon SELECT beta_testers status (esperado 401/403):", anonReadTesters.status);

  // Testa RPC publica segura de contagem agregada
  const rpcRes = await fetch(`${SUPABASE_URL}/rest/v1/rpc/get_prelaunch_stats`, {
    method: "POST",
    headers: {
      apikey: SUPABASE_ANON_KEY,
      Authorization: `Bearer ${SUPABASE_ANON_KEY}`,
      "Content-Type": "application/json",
    },
    body: "{}",
  });
  console.log("RPC get_prelaunch_stats status:", rpcRes.status, await rpcRes.text());
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
