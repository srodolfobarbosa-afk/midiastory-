#!/usr/bin/env node
/**
 * PAINEL DE CONTROLE DE TESTADORES MIDIASTORE (META 20)
 * Uso:
 *   node scripts/painel-testadores.mjs
 *   node scripts/painel-testadores.mjs --watch
 */

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const projectRef = "usmqtnlsaavxqguwrtix";
const GOAL = 20;

function resolveMcpConfigPath() {
  const localPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
  if (fs.existsSync(localPath)) return localPath;
  try {
    const gitCommonDir = execFileSync("git", ["rev-parse", "--git-common-dir"], {
      cwd: projectRoot,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
    if (gitCommonDir) {
      const mainRepoRoot = path.resolve(projectRoot, gitCommonDir, "..");
      const mainPath = path.join(mainRepoRoot, ".agents", "plugins", "supabase", "mcp_config.json");
      if (fs.existsSync(mainPath)) return mainPath;
    }
  } catch {
    // ignore
  }
  return localPath;
}

function getToken() {
  if (process.env.SUPABASE_ACCESS_TOKEN) return process.env.SUPABASE_ACCESS_TOKEN.trim();
  const mcpConfigPath = resolveMcpConfigPath();
  if (fs.existsSync(mcpConfigPath)) {
    const raw = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
    const envToken = raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN;
    if (envToken) return envToken.trim();
    const auth = raw?.mcpServers?.supabase?.headers?.Authorization || "";
    return auth.replace(/^Bearer\s+/i, "").trim();
  }
  throw new Error("Token Supabase nao encontrado.");
}

function isSyntheticTestRow(row) {
  const email = (row.email || "").toLowerCase();
  const source = (row.source || "").toLowerCase();
  return email.startsWith("teste_") || source === "automated_verification";
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

function renderProgressBar(current, goal, width = 30) {
  const ratio = Math.min(Math.max(current / goal, 0), 1);
  const filledCount = Math.round(ratio * width);
  const emptyCount = width - filledCount;
  const bar = "█".repeat(filledCount) + "░".repeat(emptyCount);
  const pct = (ratio * 100).toFixed(1);
  return `[${bar}] ${current}/${goal} (${pct}%)`;
}

function formatDate(isoStr) {
  if (!isoStr) return "-";
  const d = new Date(isoStr);
  return d.toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" });
}

async function renderDashboard() {
  const testers = await runSql(`
    SELECT id, email, source, status, created_at, synced_to_play_at
    FROM public.beta_testers
    ORDER BY id ASC;
  `);

  const realTesters = testers.filter(t => !isSyntheticTestRow(t));
  const count = realTesters.length;
  const remaining = Math.max(GOAL - count, 0);

  const exportPath = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");
  const emailsText = realTesters.map(t => t.email).join("\n");
  fs.mkdirSync(path.dirname(exportPath), { recursive: true });
  fs.writeFileSync(exportPath, emailsText, "utf8");

  console.clear();
  console.log("╔════════════════════════════════════════════════════════════════════════════════╗");
  console.log("║           🚀 PAINEL DE CONTROLE DE TESTADORES VIP — MIDIASTORE                ║");
  console.log("║                  Meta Oficial Google Play: 20 Testadores                       ║");
  console.log("╚════════════════════════════════════════════════════════════════════════════════╝\n");

  console.log(`📊 PROGRESSO DE TESTADORES REAIS NO FUNIL:`);
  console.log(`   ${renderProgressBar(count, GOAL, 35)}\n`);

  if (count >= GOAL) {
    console.log(`   🎉 META DE 20 TESTADORES REAIS ATINGIDA NO FUNIL!`);
    console.log(`   👉 Mantenha 20 testadores ativos por 14 dias contínuos na Google Play Console!\n`);
  } else {
    console.log(`   ⏳ FALTAM: ${remaining} testador(es) reais no funil para atingir os ${GOAL} exigidos pelo Google.\n`);
  }

  console.log(`📋 LISTA DE REGISTROS NO BANCO (${testers.length} total | ${count} reais):`);
  console.log("─".repeat(82));
  console.log(
    "ID".padEnd(5) +
    "E-MAIL".padEnd(35) +
    "ORIGEM".padEnd(18) +
    "STATUS".padEnd(16) +
    "DATA"
  );
  console.log("─".repeat(82));

  if (testers.length === 0) {
    console.log("   (Nenhum testador cadastrado ainda. Compartilhe o link do funil!)");
  } else {
    testers.forEach(t => {
      const id = `#${t.id}`.padEnd(5);
      const email = (t.email.length > 32 ? t.email.slice(0, 29) + "..." : t.email).padEnd(35);
      const src = (t.source || "site").slice(0, 16).padEnd(18);
      const st = (t.status || "PENDING").slice(0, 14).padEnd(16);
      const dt = formatDate(t.created_at);
      console.log(`${id}${email}${src}${st}${dt}`);
    });
  }
  console.log("─".repeat(82));

  console.log(`\n🔗 LINK DO FUNIL OFICIAL: https://midiastory-df6a0.web.app/#vip-testers`);
  console.log(`🔗 OPT-IN GOOGLE PLAY:    https://play.google.com/apps/testing/com.rdf112.midiastore`);
  console.log(`📄 LISTA EXPORTADA PARA:  docs/campaigns/lista-20-testadores-google-play.txt`);
  console.log(`🔄 Sincronizar com Play Console: node scripts/sync-play-testers.mjs\n`);
  console.log(`Última atualização: ${new Date().toLocaleTimeString("pt-BR")}`);
}

async function main() {
  const isWatch = process.argv.includes("--watch");

  if (isWatch) {
    console.log("Modo monitoramento em tempo real ativado (atualiza a cada 8s)...");
    await renderDashboard();
    setInterval(async () => {
      try {
        await renderDashboard();
      } catch (err) {
        console.error("Erro no polling:", err.message);
      }
    }, 8000);
  } else {
    await renderDashboard();
  }
}

main().catch(err => {
  console.error("Erro no painel:", err.message);
  process.exit(1);
});
