#!/usr/bin/env node
/**
 * ROTEADOR INTELIGENTE DE FAILOVER DE IA (Midiastore Engine)
 * 
 * Gerencia a execução de tarefas e prompts com cascata automática de fallback:
 * 1. Primário: OpenRouter (Gemini Flash / Argon)
 * 2. Secundário: OpenRouter (GPT-4o-mini / Modelos Leves)
 * 3. Terciário: OpenRouter Free / DeepSeek
 * 4. Local CLI: OpenAI Codex (codex.exe) em caso de falha de rede/API
 * 
 * Uso:
 *   node scripts/ai-failover-router.mjs "Descreva a lógica de limpeza de mídia duplicada"
 *   node scripts/ai-failover-router.mjs --code "Refatore o método X em WhatsAppCleanerDataSource.kt"
 */

import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");
const codexPath = "C:\\Users\\rodolfo\\AppData\\Local\\Programs\\OpenAI\\Codex\\bin\\codex.exe";

// Carregar chaves
let openRouterKey = process.env.OPENROUTER_API_KEY || "";
if (fs.existsSync(secretsPath)) {
  try {
    const raw = JSON.parse(fs.readFileSync(secretsPath, "utf8"));
    if (raw.OPENROUTER_API_KEY) openRouterKey = raw.OPENROUTER_API_KEY;
  } catch (e) {
    console.warn("Aviso ao ler segredos:", e.message);
  }
}

const MODEL_CASCADE = [
  { id: "google/gemini-3.8-flash", name: "Gemini 3.8 Flash" },
  { id: "google/gemini-3.1-flash-lite", name: "Gemini 3.1 Flash-Lite" },
  { id: "google/gemini-4-argon", name: "Gemini 4 Argon" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini" },
  { id: "meta-llama/llama-3.3-70b-instruct", name: "Llama 3.3 70B" },
];

async function callOpenRouter(modelId, prompt, maxTokens = 1500) {
  if (!openRouterKey) throw new Error("Chave OPENROUTER_API_KEY não encontrada.");
  
  const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${openRouterKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://midiastory-df6a0.web.app/",
      "X-Title": "Midiastore Failover Router",
    },
    body: JSON.stringify({
      model: modelId,
      max_tokens: maxTokens,
      messages: [{ role: "user", content: prompt }],
    }),
  });

  if (!res.ok) {
    const errText = await res.text();
    const error = new Error(`HTTP ${res.status}: ${errText}`);
    error.status = res.status;
    throw error;
  }

  const data = await res.json();
  return data.choices?.[0]?.message?.content || "";
}

function callCodexLocal(prompt) {
  if (!fs.existsSync(codexPath)) {
    throw new Error(`Codex CLI não encontrado em ${codexPath}`);
  }
  console.log("⚡ Executando via OpenAI Codex CLI Local (Zero dependência de quota externa)...");
  const cleanPrompt = prompt.replace(/"/g, '\\"');
  return execSync(
    `"${codexPath}" exec --cd "${projectRoot}" --dangerously-bypass-approvals-and-sandbox "${cleanPrompt}"`,
    { encoding: "utf8", maxBuffer: 10 * 1024 * 1024 }
  );
}

export async function executeWithFailover(prompt, options = {}) {
  const isCode = options.isCode || false;

  console.log(`[FAILOVER ROUTER] Iniciando requisição (Modo: ${isCode ? "Código/Refatoração" : "Geral"})...`);

  // Se for código direto e o usuário preferir local, ou em fallback:
  let lastError = null;

  for (let i = 0; i < MODEL_CASCADE.length; i++) {
    const model = MODEL_CASCADE[i];
    console.log(`[Rota ${i + 1}/${MODEL_CASCADE.length}] Testando modelo: ${model.name} (${model.id})...`);
    try {
      const response = await callOpenRouter(model.id, prompt, isCode ? 3500 : 2000);
      console.log(`✅ Sucesso na Rota ${i + 1} (${model.name})!`);
      return { source: model.id, content: response };
    } catch (err) {
      console.warn(`⚠️ Rota ${i + 1} (${model.name}) falhou: ${err.message.slice(0, 140)}`);
      lastError = err;
      // Tratar 429 (Rate limit), 402 (Insufficient credits), 503 (Overloaded) continuando a cascata
      continue;
    }
  }

  console.warn("⚠️ Todas as rotas de API remota esgotaram cota ou falharam. Acionando Fallback Local para o Codex CLI...");
  try {
    const localOutput = callCodexLocal(prompt);
    console.log("✅ Sucesso no Fallback Local com Codex CLI!");
    return { source: "local_codex", content: localOutput };
  } catch (codexErr) {
    console.error("❌ Falha tanto nas APIs remotas quanto no Codex local:", codexErr.message);
    throw new Error(`Falha total de failover. Último erro de API: ${lastError?.message}`);
  }
}

async function main() {
  const args = process.argv.slice(2);
  const isCode = args.includes("--code");
  const promptArgs = args.filter(a => a !== "--code");
  const prompt = promptArgs.join(" ").trim();

  if (!prompt) {
    console.log("Uso: node scripts/ai-failover-router.mjs [--code] '<prompt>'");
    process.exit(1);
  }

  const result = await executeWithFailover(prompt, { isCode });
  console.log("\n==================== RESPOSTA ====================");
  console.log(`[Provedor Vencedor: ${result.source}]`);
  console.log("==================================================");
  console.log(result.content);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(err => {
    console.error("Erro fatal:", err.message);
    process.exit(1);
  });
}
