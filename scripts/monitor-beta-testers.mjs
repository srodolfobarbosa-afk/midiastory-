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
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { getSupabaseToken as getLocalSupabaseToken } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const projectRef = "usmqtnlsaavxqguwrtix";
const GOAL = 20;

function resolveSupabaseToken() {
  try {
    return getLocalSupabaseToken();
  } catch (err) {
    try {
      const gitCommonDir = execFileSync("git", ["rev-parse", "--git-common-dir"], {
        cwd: projectRoot,
        encoding: "utf8",
        stdio: ["ignore", "pipe", "ignore"],
      }).trim();
      if (gitCommonDir) {
        const mainRepoRoot = path.resolve(projectRoot, gitCommonDir, "..");
        const mcpConfigPath = path.join(mainRepoRoot, ".agents", "plugins", "supabase", "mcp_config.json");
        if (fs.existsSync(mcpConfigPath)) {
          const config = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
          const envToken = config?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN?.trim();
          if (envToken) return envToken;
          const authHeader = config?.mcpServers?.supabase?.headers?.Authorization || "";
          const bearerToken = authHeader.replace(/^Bearer\s+/i, "").trim();
          if (bearerToken) return bearerToken;
        }
      }
    } catch {
      // ignore fallback error
    }
    throw err;
  }
}

function isSyntheticTestRow(row) {
  const email = (row.email || "").toLowerCase();
  const source = (row.source || "").toLowerCase();
  return email.startsWith("teste_") || source === "automated_verification";
}

async function runSql(query) {
  const token = resolveSupabaseToken();
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

    const realTesters = testers.filter(t => !isSyntheticTestRow(t));
    const count = realTesters.length;
    const remaining = Math.max(GOAL - count, 0);

    const now = new Date().toLocaleTimeString("pt-BR");
    console.log(`[${now}] === STATUS DE RECRUTAMENTO MIDIASTORE ===`);
    console.log(`Testadores Reais no Funil: ${count}/${GOAL} (${((count / GOAL) * 100).toFixed(0)}%) | Total linhas: ${testers.length}`);
    console.log(`Faltam no Funil para Meta de 20: ${remaining} testador(es)`);

    if (testers.length > 0) {
      console.log("Últimos inscritos:");
      testers.slice(-5).forEach(t => {
        const tag = isSyntheticTestRow(t) ? " [SINTÉTICO]" : "";
        console.log(` - [#${t.id}] ${t.email} (${t.source || "site"}) [${t.status || "PENDING"}]${tag}`);
      });
    }

    const exportPath = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");
    const emailsText = realTesters.map(t => t.email).join("\n");
    fs.mkdirSync(path.dirname(exportPath), { recursive: true });
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
