#!/usr/bin/env node
/**
 * Monitor Autônomo de Beta Testers (Midiastore -> Supabase)
 * Consulta a tabela public.beta_testers para acompanhar as novas entradas
 * vindas do funil VIP (https://midiastory-df6a0.web.app/#vip-testers).
 *
 * Uso:
 *   node scripts/monitor-beta-testers.mjs
 *   node scripts/monitor-beta-testers.mjs --watch
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const mcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
const projectRef = "usmqtnlsaavxqguwrtix";

function getToken() {
  if (process.env.SUPABASE_ACCESS_TOKEN) return process.env.SUPABASE_ACCESS_TOKEN;
  if (fs.existsSync(mcpConfigPath)) {
    try {
      const raw = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
      const envToken = raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN;
      if (envToken) return envToken.trim();
      const auth = raw?.mcpServers?.supabase?.headers?.Authorization || "";
      return auth.replace(/^Bearer\s+/i, "").trim();
    } catch {
      // fallback
    }
  }
  // Token conhecido do projeto
  return "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
}

async function runSql(query) {
  const token = getToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

async function checkBetaTesters() {
  try {
    // 1. Verificar se a tabela beta_testers existe
    const tableCheck = await runSql(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' AND table_name = 'beta_testers';
    `);

    if (!tableCheck || tableCheck.length === 0) {
      console.log("[INFO] Tabela public.beta_testers ainda não existe no Supabase.");
      console.log("[SETUP] Criando estrutura recomendada da tabela public.beta_testers...");
      await runSql(`
        CREATE TABLE IF NOT EXISTS public.beta_testers (
          id BIGSERIAL PRIMARY KEY,
          email TEXT UNIQUE NOT NULL,
          name TEXT,
          device_model TEXT,
          android_version TEXT,
          source_channel TEXT DEFAULT 'web_vip_funnel',
          reciprocal_app_name TEXT,
          reciprocal_play_link TEXT,
          status TEXT DEFAULT 'pending_opt_in', -- pending_opt_in, opted_in, active_14_days, completed
          registered_at TIMESTAMPTZ DEFAULT NOW(),
          last_active_at TIMESTAMPTZ,
          notes TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_beta_testers_email ON public.beta_testers (email);
        CREATE INDEX IF NOT EXISTS idx_beta_testers_status ON public.beta_testers (status);
      `);
      console.log("[SETUP] Tabela public.beta_testers criada com sucesso!");
    }

    // 2. Consultar testadores cadastrados
    const testers = await runSql(`
      SELECT id, email, name, device_model, source_channel, reciprocal_app_name, status, registered_at
      FROM public.beta_testers
      ORDER BY registered_at DESC
      LIMIT 50;
    `);

    const countRes = await runSql(`
      SELECT 
        COUNT(*) as total,
        COUNT(CASE WHEN status = 'opted_in' OR status = 'active_14_days' THEN 1 END) as active_count,
        COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_count
      FROM public.beta_testers;
    `);

    const stats = countRes[0] || { total: 0, active_count: 0, completed_count: 0 };

    console.log("\n=======================================================");
    console.log("   MIDIASTORE - PAINEL DE MONITORAMENTO DE TESTADORES   ");
    console.log("=======================================================");
    console.log(`Meta Google Play: 20 testadores por 14 dias`);
    console.log(`Total Cadastrado: ${stats.total} | Ativos: ${stats.active_count} | Concluídos: ${stats.completed_count}`);
    console.log("-------------------------------------------------------");

    if (testers.length === 0) {
      console.log("Nenhum testador registrado ainda no banco.");
      console.log("Divulgue o link do funil VIP oficial:");
      console.log("👉 https://midiastory-df6a0.web.app/#vip-testers");
    } else {
      testers.forEach((t, i) => {
        console.log(`[#${i + 1}] ID:${t.id} | ${t.email} (${t.name || "Sem Nome"})`);
        console.log(`    Status: ${t.status} | Canal: ${t.source_channel || "Direto"}`);
        if (t.reciprocal_app_name) {
          console.log(`    App Mútuo: ${t.reciprocal_app_name}`);
        }
        console.log(`    Cadastro: ${t.registered_at}`);
      });
    }
    console.log("=======================================================\n");

    return { stats, testers };
  } catch (err) {
    console.error("[ERRO ao monitorar beta_testers]:", err.message);
    throw err;
  }
}

async function main() {
  const args = process.argv.slice(2);
  const isWatch = args.includes("--watch");

  await checkBetaTesters();

  if (isWatch) {
    console.log("[POLLING] Monitoramento em loop ativo (a cada 30 segundos)...");
    setInterval(async () => {
      try {
        await checkBetaTesters();
      } catch (e) {
        console.error("Erro na verificação periódica:", e.message);
      }
    }, 30000);
  }
}

main().catch((err) => {
  console.error("Falha na execução:", err);
  process.exit(1);
});
