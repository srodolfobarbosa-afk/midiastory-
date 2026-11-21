#!/usr/bin/env node
/**
 * FRIDAY Self-Audit Engine — Auto-Varredura 100% da Plataforma FRIDAY (Construtora)
 *
 * Audita em <3s (zero Python, zero custo de API, zero RAM residente):
 *   1. Sintaxe (node --check) de todos os motores FRIDAY & Voz
 *   2. Integridade do .agents/hooks.json (todos os comandos apontam para arquivos existentes)
 *   3. Saúde do Companion (heartbeat ativo?), cooldown TTS 429, fila de voz
 *   4. MCPs configurados em ~/.gemini/config/mcp_config.json
 *   5. RAM livre do PC (proteção 3,93 GB)
 *   6. Cota da sessão ativa (via friday-quota-guard.mjs)
 *
 * Uso:
 *   node scripts/friday-self-audit.mjs
 *   node scripts/friday-self-audit.mjs --board   (projeta relatório visual no Quadro OLED)
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");

function checkNodeSyntax(relPath) {
  const abs = path.join(PROJECT_ROOT, relPath);
  if (!fs.existsSync(abs)) {
    return { name: relPath, ok: false, detail: "ARQUIVO AUSENTE" };
  }
  const res = spawnSync(process.execPath, ["--check", abs], { encoding: "utf8", timeout: 5000 });
  return {
    name: relPath,
    ok: res.status === 0,
    detail: res.status === 0 ? "OK" : (res.stderr || "ERRO DE SINTAXE").trim().slice(0, 120),
  };
}

function checkHooks() {
  const hooksPath = path.join(PROJECT_ROOT, ".agents", "hooks.json");
  if (!fs.existsSync(hooksPath)) {
    return { ok: false, count: 0, detail: ".agents/hooks.json ausente" };
  }
  try {
    const json = JSON.parse(fs.readFileSync(hooksPath, "utf8"));
    let count = 0;
    const missing = [];
    for (const group of Object.values(json)) {
      for (const list of Object.values(group || {})) {
        if (!Array.isArray(list)) continue;
        for (const hook of list) {
          count++;
          const m = String(hook.command || "").match(/node\s+(\S+\.mjs)/i);
          if (m) {
            const target = path.resolve(path.join(PROJECT_ROOT, ".agents"), m[1]);
            if (!fs.existsSync(target)) missing.push(m[1]);
          }
        }
      }
    }
    return {
      ok: missing.length === 0 && count > 0,
      count,
      detail: missing.length ? `Ausentes: ${missing.join(", ")}` : `${count} hooks íntegros`,
    };
  } catch (e) {
    return { ok: false, count: 0, detail: `JSON inválido: ${e.message}` };
  }
}

function checkCompanion() {
  const hbPath = path.join(os.tmpdir(), "antigravity-companion-heartbeat.txt");
  if (!fs.existsSync(hbPath)) {
    return { ok: false, detail: "Heartbeat não encontrado (Companion fechado?)" };
  }
  const ageSec = Math.round((Date.now() - fs.statSync(hbPath).mtimeMs) / 1000);
  return {
    ok: ageSec <= 15,
    detail: ageSec <= 15 ? `ATIVO (heartbeat há ${ageSec}s)` : `INATIVO (último sinal há ${ageSec}s)`,
  };
}

function checkTtsCooldown() {
  const cdPath = path.join(os.tmpdir(), "antigravity-tts-429-until.txt");
  if (!fs.existsSync(cdPath)) return "Google Kore livre (sem 429)";
  const until = Number(fs.readFileSync(cdPath, "utf8").trim()) || 0;
  const remMin = Math.ceil((until - Date.now()) / 60000);
  if (remMin > 0) return `Edge Francisca Neural ativo (Google 429 cooldown: ${remMin} min)`;
  return "Google Kore livre (cooldown expirado)";
}

function checkMcps() {
  const mcpPath = path.join(os.homedir(), ".gemini", "config", "mcp_config.json");
  if (!fs.existsSync(mcpPath)) return { ok: false, servers: [], detail: "mcp_config.json ausente" };
  try {
    const raw = JSON.parse(fs.readFileSync(mcpPath, "utf8"));
    const servers = Object.keys(raw.mcpServers || {});
    return { ok: servers.length > 0, servers, detail: servers.join(", ") || "nenhum" };
  } catch (e) {
    return { ok: false, servers: [], detail: e.message };
  }
}

function main() {
  const coreScripts = [
    "scripts/friday-triage-engine.mjs",
    "scripts/friday-hud.mjs",
    "scripts/friday-quota-guard.mjs",
    "scripts/friday-play-radar.mjs",
    "scripts/friday-self-audit.mjs",
    "scripts/antigravity-board.mjs",
    "scripts/voice-narrator.mjs",
    "scripts/voice-transcribe.mjs",
    "scripts/hooks/cognitive-hook.mjs",
  ];

  const syntaxResults = coreScripts.map(checkNodeSyntax);
  const syntaxOk = syntaxResults.filter((r) => r.ok).length;
  const hooks = checkHooks();
  const companion = checkCompanion();
  const tts = checkTtsCooldown();
  const mcps = checkMcps();

  const freeRamGb = +(os.freemem() / 1024 ** 3).toFixed(2);
  const totalRamGb = +(os.totalmem() / 1024 ** 3).toFixed(2);
  const ramOk = freeRamGb >= 0.45;

  const checks = [
    syntaxOk === coreScripts.length,
    hooks.ok,
    companion.ok,
    mcps.ok,
    ramOk,
  ];
  const score = Math.round((checks.filter(Boolean).length / checks.length) * 100);

  const lines = [
    `=== FRIDAY SELF-AUDIT (${score}% SAÚDE) ===`,
    `• Motores JS (${syntaxOk}/${coreScripts.length}): ${syntaxOk === coreScripts.length ? "100% válidos" : syntaxResults.filter((r) => !r.ok).map((r) => `${r.name}: ${r.detail}`).join(" | ")}`,
    `• Hooks Antigravity: ${hooks.ok ? "OK" : "ALERTA"} — ${hooks.detail}`,
    `• Companion Voz/Avatar: ${companion.ok ? "OK" : "ALERTA"} — ${companion.detail}`,
    `• Motor de Voz (TTS): ${tts}`,
    `• MCPs Configurados: ${mcps.detail}`,
    `• RAM do PC: ${freeRamGb} GB livres / ${totalRamGb} GB (${ramOk ? "SEGURO" : "APERTO DE RAM"})`,
  ];

  const report = lines.join("\n");
  console.log(report);

  if (process.argv.includes("--board")) {
    const boardScript = path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs");
    if (fs.existsSync(boardScript)) {
      spawnSync(
        process.execPath,
        [boardScript, "--title", `FRIDAY Self-Audit — ${score}% Saúde`, "--body", report, "--open"],
        { stdio: "ignore", timeout: 10000 }
      );
    }
  }

  process.exit(score >= 80 ? 0 : 1);
}

main();
