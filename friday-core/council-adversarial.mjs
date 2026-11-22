#!/usr/bin/env node
/**
 * FRIDAY Core — 1. Conselho Adversarial de Dois Cérebros (Gemini vs Codex CLI / OpenRouter)
 *
 * Fluxo real:
 *   1. Cérebro A (Arquiteto — Google AI Studio Gemini) propõe plano técnico/estratégico objetivo.
 *   2. Cérebro B (Advogado do Diabo — `codex.exe` OpenAI local, com fallback automático para OpenRouter `:free`)
 *      ataca o plano buscando bugs, riscos legais/Play Store, custos ocultos e furos de mercado.
 *   3. Síntese Final: consolida o plano sobrevivente, salva log em `friday-core/memory/council-logs/`
 *      e projeta no Quadro OLED se `--board` for passado.
 *
 * Uso:
 *   node friday-core/council-adversarial.mjs "Proposta ou decisão crítica" [--board] [--fast]
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const SECRETS_PATH = path.join(PROJECT_ROOT, ".secrets", "ai-media-keys.json");
const LOGS_DIR = path.join(__dirname, "memory", "council-logs");

function loadSecrets() {
  let obj = {};
  if (fs.existsSync(SECRETS_PATH)) {
    try {
      obj = JSON.parse(fs.readFileSync(SECRETS_PATH, "utf8"));
    } catch {}
  }
  return {
    googleKey:
      process.env.GOOGLE_AI_STUDIO_API_KEY ||
      process.env.GOOGLE_AQ_API_KEY ||
      process.env.GEMINI_API_KEY ||
      obj.GOOGLE_AI_STUDIO_API_KEY ||
      obj.GOOGLE_AQ_API_KEY ||
      "",
    openRouterKey: process.env.OPENROUTER_API_KEY || obj.OPENROUTER_API_KEY || "",
  };
}

async function callGemini(prompt, googleKey) {
  if (!googleKey) return { ok: false, engine: "gemini", error: "GOOGLE_API_KEY ausente" };
  // Prioridade: Carbon (quando billing ativo) -> Hydrogen (1M ctx GRÁTIS verificado) -> 2.5-flash -> 3.8-flash
  const models = ["gemini-4-carbon", "gemini-4-hydrogen", "gemini-2.5-flash", "gemini-3.8-flash"];
  let lastErr = "";

  for (const model of models) {
    try {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${googleKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          signal: AbortSignal.timeout(25000),
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { temperature: 0.3, maxOutputTokens: 900 },
          }),
        }
      );
      if (!res.ok) {
        lastErr = `${model}:HTTP_${res.status}`;
        continue;
      }
      const data = await res.json();
      const text = String(data?.candidates?.[0]?.content?.parts?.[0]?.text || "").trim();
      if (text) return { ok: true, engine: `Google AI Studio (${model})`, text };
    } catch (e) {
      lastErr = `${model}:${e.name || e.message}`;
    }
  }
  return { ok: false, engine: "gemini", error: lastErr || "sem resposta" };
}

function callCodexLocal(prompt) {
  const which = spawnSync("where.exe", ["codex.exe"], { encoding: "utf8", timeout: 4000 });
  if (which.status !== 0) {
    return { ok: false, engine: "codex.exe", error: "codex.exe não encontrado no PATH" };
  }
  const codexBin = which.stdout.split(/\r?\n/).find(Boolean)?.trim() || "codex.exe";
  const res = spawnSync(codexBin, ["exec", "--skip-git-repo-check", prompt], {
    cwd: PROJECT_ROOT,
    encoding: "utf8",
    timeout: 45000,
    maxBuffer: 2 * 1024 * 1024,
  });
  const out = String(res.stdout || "").trim();
  if (res.status === 0 && out.length > 20) {
    return { ok: true, engine: "OpenAI Codex CLI (local)", text: out.slice(-2500) };
  }
  return {
    ok: false,
    engine: "codex.exe",
    error: (res.stderr || res.stdout || `exit ${res.status}`).trim().slice(0, 180),
  };
}

async function callOpenRouterFree(prompt, apiKey) {
  if (!apiKey) return { ok: false, engine: "openrouter:free", error: "OPENROUTER_API_KEY ausente" };
  const models = ["openrouter/free", "nvidia/nemotron-3.5-lightning:free", "google/gemma-4-31b-it:free"];
  let lastErr = "";
  for (const model of models) {
    try {
      const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        signal: AbortSignal.timeout(25000),
        body: JSON.stringify({
          model,
          max_tokens: 700,
          temperature: 0.3,
          messages: [{ role: "user", content: prompt }],
        }),
      });
      if (!res.ok) {
        lastErr = `${model}:HTTP_${res.status}`;
        continue;
      }
      const data = await res.json();
      const text = String(data?.choices?.[0]?.message?.content || "").trim();
      if (text) return { ok: true, engine: `OpenRouter (${model})`, text };
    } catch (e) {
      lastErr = `${model}:${e.name || e.message}`;
    }
  }
  return { ok: false, engine: "openrouter:free", error: lastErr || "sem resposta" };
}

async function main() {
  const args = process.argv.slice(2);
  const openBoard = args.includes("--board");
  const fastMode = args.includes("--fast");
  const topic = args.filter((a) => !a.startsWith("--")).join(" ").trim();

  if (!topic) {
    console.log('Uso: node friday-core/council-adversarial.mjs "<decisão ou proposta>" [--board] [--fast]');
    process.exit(1);
  }

  const secrets = loadSecrets();
  console.log(`\n⚔️ CONSELHO ADVERSARIAL FRIDAY — Tema: "${topic}"\n`);

  // Rodada 1: Cérebro A (Arquiteto)
  const architectPrompt = [
    "Você é o Cérebro A (Arquiteto Construtor) da plataforma FRIDAY.",
    "Regras: zero Python (só Node.js/Kotlin/CLIs nativas), custo extra R$ 0, verdade absoluta, foco em qualidade e receita real.",
    `Proposta/Decisão a desenhar: ${topic}`,
    "Entregue um plano direto em no máximo 5 bullets objetivos (o quê, como validar, risco principal).",
  ].join("\n");

  const brainA = await callGemini(architectPrompt, secrets.googleKey);
  console.log(`🧠 CÉREBRO A [${brainA.engine}]:`);
  console.log(brainA.ok ? brainA.text : `FALHA: ${brainA.error}`);

  // Rodada 2: Cérebro B (Advogado do Diabo — Codex local -> fallback OpenRouter :free -> fallback Gemini crítico)
  const criticPrompt = [
    "Você é o Cérebro B (Advogado do Diabo Implacável).",
    `Tema original: ${topic}`,
    `Plano proposto pelo Cérebro A:\n${brainA.ok ? brainA.text : "(Cérebro A indisponível)"}`,
    "Sua missão: ATACAR este plano sem piedade. Liste os 3 maiores furos técnicos, de custo, compliance ou mercado, e proponha 1 ajuste cirúrgico que torna o plano à prova de balas. Máximo 180 palavras, português do Brasil.",
  ].join("\n\n");

  let brainB = fastMode ? { ok: false, engine: "skipped-fast", error: "modo --fast" } : callCodexLocal(criticPrompt);
  if (!brainB.ok) {
    const orRes = await callOpenRouterFree(criticPrompt, secrets.openRouterKey);
    brainB = orRes.ok ? orRes : await callGemini(criticPrompt, secrets.googleKey);
  }

  console.log(`\n🛡️ CÉREBRO B — ADVOGADO DO DIABO [${brainB.engine}]:`);
  console.log(brainB.ok ? brainB.text : `FALHA: ${brainB.error}`);

  // Persistir log estruturado
  fs.mkdirSync(LOGS_DIR, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
  const logFile = path.join(LOGS_DIR, `council-${stamp}.json`);
  const record = {
    timestamp: new Date().toISOString(),
    topic,
    brainA,
    brainB,
  };
  fs.writeFileSync(logFile, JSON.stringify(record, null, 2), "utf8");
  console.log(`\n📁 Log salvo em: ${path.relative(PROJECT_ROOT, logFile)}`);

  if (openBoard) {
    const boardScript = path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs");
    if (fs.existsSync(boardScript)) {
      const summary = [
        `TEMA: ${topic}`,
        "",
        `[CÉREBRO A — ${brainA.engine}]`,
        brainA.ok ? brainA.text : brainA.error,
        "",
        `[CÉREBRO B — ADVOGADO DO DIABO — ${brainB.engine}]`,
        brainB.ok ? brainB.text : brainB.error,
      ].join("\n");
      spawnSync(
        process.execPath,
        [boardScript, "--title", "⚔️ Conselho Adversarial FRIDAY", "--body", summary, "--open"],
        { stdio: "ignore", timeout: 10000 }
      );
    }
  }
}

main().catch((e) => {
  console.error("COUNCIL_ERROR:", e.message);
  process.exit(1);
});
