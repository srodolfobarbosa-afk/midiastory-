#!/usr/bin/env node
/**
 * FRIDAY Mission Control HUD — Real-Time Executive Briefing for Midiastore
 *
 * Pulls LIVE telemetry (zero stale data!) from:
 *   1. Supabase (beta_testers, play_purchases)
 *   2. GitHub Actions (latest CI run via gh CLI)
 *   3. Motorola USB (live adb devices)
 *   4. Local PC Health (RAM & Disk)
 *
 * Projects the live OLED Mission HUD onto the Antigravity Professor Board
 * and prints a crisp executive summary for voice narration.
 *
 * Usage:
 *   node scripts/friday-hud.mjs [--open]
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { execSync, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");

function safeExec(cmd, timeout = 8000) {
  try {
    return execSync(cmd, { cwd: PROJECT_ROOT, encoding: "utf8", timeout, stdio: ["ignore", "pipe", "ignore"] }).trim();
  } catch {
    return "";
  }
}

function getPcHealth() {
  const totalGb = (os.totalmem() / 1024 ** 3).toFixed(2);
  const freeGb = (os.freemem() / 1024 ** 3).toFixed(2);
  const freePct = Math.round((os.freemem() / os.totalmem()) * 100);
  return { totalGb, freeGb, freePct };
}

function getMotorolaStatus() {
  const adb = "C:\\Users\\rodolfo\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe";
  if (!fs.existsSync(adb)) return "ADB não instalado";
  const out = safeExec(`"${adb}" devices`, 4000);
  const lines = out.split(/\r?\n/).filter((l) => /\tdevice\b/.test(l));
  if (lines.length === 0) return "Desconectado (conecte o USB para QA/espelhamento)";
  const serial = lines[0].split(/\s+/)[0];
  const model = safeExec(`"${adb}" -s ${serial} shell getprop ro.product.model`, 3000) || serial;
  const battRaw = safeExec(`"${adb}" -s ${serial} shell dumpsys battery`, 3000);
  const lvl = (battRaw.match(/level:\s*(\d+)/) || [])[1];
  return `Conectado: ${model} (${serial})${lvl ? ` — Bateria ${lvl}%` : ""}`;
}

function getLatestCiStatus() {
  const raw = safeExec(`gh run list --limit 1 --json databaseId,displayTitle,status,conclusion,headBranch,createdAt`, 8000);
  if (!raw) return "Indisponível (gh offline)";
  try {
    const arr = JSON.parse(raw);
    if (!arr.length) return "Sem runs recentes";
    const r = arr[0];
    return `#${r.databaseId} (${r.headBranch}) — ${r.conclusion || r.status}: ${r.displayTitle}`;
  } catch {
    return "Erro ao ler CI";
  }
}

async function getSupabaseLive() {
  try {
    const token = getSupabaseToken();
    if (!token) return { testers: "N/D", purchases: "N/D" };
    const r = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      signal: AbortSignal.timeout(7000),
      body: JSON.stringify({
        query: "SELECT (SELECT count(*) FROM public.beta_testers) AS testers, (SELECT count(*) FROM public.play_purchases) AS purchases;",
      }),
    });
    if (!r.ok) return { testers: "erro", purchases: "erro" };
    const rows = await r.json();
    const row = Array.isArray(rows) ? rows[0] : null;
    return {
      testers: row?.testers ?? "N/D",
      purchases: row?.purchases ?? "N/D",
    };
  } catch {
    return { testers: "N/D", purchases: "N/D" };
  }
}

async function main() {
  const openBoard = process.argv.includes("--open");
  const pc = getPcHealth();
  const moto = getMotorolaStatus();
  const ci = getLatestCiStatus();
  const supa = await getSupabaseLive();
  const gitBranch = safeExec("git rev-parse --abbrev-ref HEAD", 3000) || "desconhecido";
  const gitHead = safeExec("git log -1 --oneline", 3000) || "";

  const body = [
    "## 🚀 Telemetria Ao Vivo do Ecossistema Midiastore",
    `- **GitHub CI/CD:** ${ci}`,
    `- **Branch & Último Commit:** ${gitBranch} — ${gitHead}`,
    `- **Supabase Ao Vivo:** ${supa.testers} registros em beta_testers | ${supa.purchases} compras verificadas em play_purchases`,
    `- **Hardware Motorola USB:** ${moto}`,
    `- **Saúde do PC Local:** ${pc.freeGb} GB livres de ${pc.totalGb} GB (${pc.freePct}% livre)`,
    "",
    "## 🎯 Meta Suprema: R$ 1.000.000,00 na Google Play",
    "- **Regra 50/50:** 50% Lucro/Retirada + 50% Reinvestimento em Tráfego, IA e Escala.",
    "- **Comandos de Voz FRIDAY:** diga \"relatório geral\", \"espiona os concorrentes na Play Store\", \"me ensina no quadro\" ou clique em **👁️Tela**.",
  ].join("\n");

  const boardArgs = [
    path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs"),
    "--title",
    "FRIDAY Mission HUD — Midiastore Ao Vivo",
    "--subtitle",
    `Verificado em tempo real às ${new Date().toLocaleTimeString("pt-BR")} (Zero dado velho)`,
    "--body",
    body,
    "--image",
    "",
  ];
  if (openBoard) boardArgs.push("--open");
  spawnSync(process.execPath, boardArgs, { cwd: PROJECT_ROOT, stdio: "inherit" });

  console.log(
    JSON.stringify({ pc, moto, ci, supa, gitBranch, gitHead }, null, 2)
  );
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
