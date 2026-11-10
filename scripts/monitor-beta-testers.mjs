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
const GOAL = 20;

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
    const testers = await runSql(`
      SELECT id, email, source, status, created_at, synced_to_play_at
      FROM public.beta_testers
      ORDER BY id ASC;
    `);

    const count = testers.length;
    const remaining = Math.max(GOAL - count, 0);

    const now = new Date().toLocaleTimeString("pt-BR");
    console.log(`[${now}] === STATUS DE RECRUTAMENTO MIDIASTORE ===`);
    console.log(`Testadores Registrados: ${count}/${GOAL} (${((count / GOAL) * 100).toFixed(0)}%)`);
    console.log(`Faltam para liberação de Produção: ${remaining} testador(es)`);

    if (testers.length > 0) {
      console.log("Últimos inscritos:");
      testers.slice(-5).forEach(t => {
        console.log(` - [#${t.id}] ${t.email} (${t.source || "site"}) [${t.status || "PENDING"}]`);
      });
    }

    // Exportar lista atualizada para arquivo
    const exportPath = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");
    const emailsText = testers.map(t => t.email).join("\n");
    fs.writeFileSync(exportPath, emailsText, "utf8");
  } catch (err) {
    console.error("[ERRO ao monitorar beta_testers]:", err.message);
  }
}

async function main() {
  const args = process.argv.slice(2);
  const isWatch = args.includes("--watch");

  await checkBetaTesters();

  if (isWatch) {
    console.log("[POLLING] Monitoramento contínuo em segundo plano ativo (intervalo: 60s)...");
    setInterval(async () => {
      await checkBetaTesters();
    }, 60000);
  }
}

main().catch((err) => {
  console.error("Falha na execução:", err);
  process.exit(1);
});
