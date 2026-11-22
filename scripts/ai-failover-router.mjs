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
import { execSync, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import {
  RODOLFO_BRAIN_SYSTEM_PROMPT,
  RODOLFO_BRAIN_MODEL_PRIORITY,
  buildCodexPromptWithPersona,
  buildRodolfoBrainSystemPrompt,
} from "./lib/rodolfo-brain-persona.mjs";
import { analyzeAndLearnTurn, learnFromInteraction } from "./lib/cognitive-memory.mjs";
import { ProviderCircuitBreaker } from "./lib/circuit-breaker.mjs";
import { GoogleGenAI } from "@google/genai";
import * as v from "valibot";

const GoogleCallOptionsSchema = v.object({
  maxTokens: v.optional(v.number()),
  responseFormatJson: v.optional(v.boolean()),
  thinkingLevel: v.optional(v.string()),
  isCode: v.optional(v.boolean()),
  systemPrompt: v.optional(v.string()),
  timeoutMs: v.optional(v.number()),
});

const circuitBreaker = new ProviderCircuitBreaker();

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");
const codexPath = "C:\\Users\\rodolfo\\AppData\\Local\\Programs\\OpenAI\\Codex\\bin\\codex.exe";

// Carregar chaves
let openRouterKey = process.env.OPENROUTER_API_KEY || "";
let googleStudioKey = process.env.GOOGLE_AI_STUDIO_API_KEY || process.env.GOOGLE_AQ_API_KEY || "";

if (process.env.AI_MEDIA_KEYS_JSON) {
  try {
    const parsed = JSON.parse(process.env.AI_MEDIA_KEYS_JSON);
    if (parsed.OPENROUTER_API_KEY) openRouterKey = parsed.OPENROUTER_API_KEY;
    if (parsed.GOOGLE_AI_STUDIO_API_KEY) googleStudioKey = parsed.GOOGLE_AI_STUDIO_API_KEY;
    else if (parsed.GOOGLE_AQ_API_KEY) googleStudioKey = parsed.GOOGLE_AQ_API_KEY;
  } catch (e) {}
}
if (fs.existsSync(secretsPath)) {
  try {
    const raw = JSON.parse(fs.readFileSync(secretsPath, "utf8"));
    if (!openRouterKey && raw.OPENROUTER_API_KEY) openRouterKey = raw.OPENROUTER_API_KEY;
    if (!googleStudioKey) {
      if (raw.GOOGLE_AI_STUDIO_API_KEY) googleStudioKey = raw.GOOGLE_AI_STUDIO_API_KEY;
      else if (raw.GOOGLE_AQ_API_KEY) googleStudioKey = raw.GOOGLE_AQ_API_KEY;
    }
  } catch (e) {
    console.warn("Aviso ao ler segredos:", e.message);
  }
}

let googleGenAiClient = null;
function getGoogleGenAiClient() {
  if (!googleGenAiClient && googleStudioKey) {
    googleGenAiClient = new GoogleGenAI({
      apiKey: googleStudioKey,
      httpOptions: { apiVersion: "v1beta" },
    });
  }
  return googleGenAiClient;
}

async function callGoogleAIStudio(modelName, prompt, rawOptions = {}) {
  if (!googleStudioKey) throw new Error("Chave GOOGLE_AI_STUDIO_API_KEY não encontrada.");
  const options = v.parse(GoogleCallOptionsSchema, rawOptions || {});
  const isGemini4Thinking =
    modelName.includes("gemini-4-neon") ||
    modelName.includes("gemini-4-hydrogen") ||
    modelName.includes("gemini-4-carbon") ||
    modelName.includes("gemini-4-argon");
  const thinkingLevel = options.thinkingLevel || (options.isCode ? "HIGH" : "MEDIUM");

  const config = {
    systemInstruction: options.systemPrompt || RODOLFO_BRAIN_SYSTEM_PROMPT,
    ...(options.maxTokens ? { maxOutputTokens: options.maxTokens } : {}),
    ...(options.responseFormatJson ? { responseMimeType: "application/json" } : {}),
    ...(isGemini4Thinking ? { thinkingConfig: { thinkingLevel } } : {}),
    abortSignal: AbortSignal.timeout(options.timeoutMs || 25000),
  };

  const ai = getGoogleGenAiClient();
  const response = await ai.models.generateContent({
    model: modelName,
    contents: prompt,
    config,
  });

  const text = String(response?.text || "").trim();
  if (text) return text;
  const parts = response?.candidates?.[0]?.content?.parts || [];
  return parts.map((p) => p.text || "").join("").trim();
}

const MODEL_CASCADE = [
  // 1. Modelos Frontier com Raciocínio (Tentativa primária com auto-adaptação e BYOK)
  { id: "google/gemini-4-neon", name: "Gemini 4 Neon (Frontier Reasoning)" },
  { id: "google/gemini-4-carbon", name: "Gemini 4 Carbon (1M Context + High Reasoning)" },
  { id: "google/gemini-4-argon", name: "Gemini 4 Argon" },
  { id: "google/gemini-3.8-flash", name: "Gemini 3.8 Flash" },
  { id: "google/gemini-3.1-flash-lite", name: "Gemini 3.1 Flash-Lite" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini" },

  // 2. Modelos Gratuitos Validados no OpenRouter (Custo Zero, Sem 402)
  { id: "nvidia/nemotron-3.5-lightning:free", name: "NVIDIA Nemotron 3.5 Lightning (Free)" },
  { id: "openrouter/free", name: "OpenRouter Free Auto-Router" },
  { id: "google/gemma-4-31b-it:free", name: "Google Gemma 4 31B (Free)" },
  { id: "nvidia/nemotron-3-ultra-550b-a55b:free", name: "NVIDIA Nemotron 3 Ultra 550B (Free)" },
  { id: "nvidia/nemotron-3.5-super-vl:free", name: "NVIDIA Nemotron 3.5 Super VL (Free)" },
  { id: "cohere/north-mini-code:free", name: "Cohere North Mini Code (Free)" },
  { id: "baidu/ernie-5.5-flash-preview:free", name: "Baidu ERNIE 5.5 Flash (Free)" },
  { id: "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", name: "NVIDIA Nemotron 3 Nano Omni (Free)" },
];

const GITHUB_MODELS_CASCADE = [
  "openai/gpt-4o",
  "openai/gpt-4o-mini",
  "deepseek/DeepSeek-R1",
];

function getGitHubToken() {
  return process.env.GITHUB_MODELS_TOKEN?.trim() || "";
}

async function callGitHubModels(modelId, prompt, maxTokens = 1200, responseFormatJson = false, systemPrompt = RODOLFO_BRAIN_SYSTEM_PROMPT) {
  const token = getGitHubToken();
  if (!token) throw new Error("GITHUB_MODELS_TOKEN (PAT models:read) não configurado.");
  const res = await fetch("https://models.github.ai/inference/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    signal: AbortSignal.timeout(25000),
    body: JSON.stringify({
      model: modelId,
      max_tokens: maxTokens,
      ...(responseFormatJson && modelId.startsWith("openai/") ? { response_format: { type: "json_object" } } : {}),
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: prompt },
      ],
    }),
  });
  if (!res.ok) {
    const errText = await res.text();
    const error = new Error(`HTTP ${res.status}: ${errText}`);
    error.status = res.status;
    throw error;
  }
  const ct = res.headers.get("content-type") || "";
  if (!ct.includes("application/json")) {
    throw new Error(`Resposta não-JSON de GitHub Models (${ct}): ${(await res.text()).slice(0, 80)}`);
  }
  const data = await res.json();
  return data.choices?.[0]?.message?.content || "";
}

async function callOpenRouter(modelId, prompt, maxTokens = 1200, reasoningEffort = "medium", responseFormatJson = false, systemPrompt = RODOLFO_BRAIN_SYSTEM_PROMPT) {
  if (!openRouterKey) throw new Error("Chave OPENROUTER_API_KEY não encontrada.");
  
  const isReasoningModel = modelId.includes("carbon") || modelId.includes("argon") || modelId.includes("nano-omni");
  const isFreeModel = modelId.endsWith(":free") || modelId === "openrouter/free";

  let res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    signal: AbortSignal.timeout(20000),
    headers: {
      Authorization: `Bearer ${openRouterKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://midiastory-df6a0.web.app/",
      "X-Title": "Midiastore Failover Router",
    },
    body: JSON.stringify({
      model: modelId,
      max_tokens: maxTokens,
      ...(responseFormatJson && !isFreeModel ? { response_format: { type: "json_object" } } : {}),
      ...(isReasoningModel ? { reasoning: { effort: reasoningEffort } } : {}),
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: prompt },
      ],
    }),
  });

  // Auto-adaptação para HTTP 402 (crédito insuficiente para reserva alta de tokens)
  if (res.status === 402) {
    const errText = await res.text();
    const match = errText.match(/can only afford (\d+)/i);
    const maxAffordable = match ? parseInt(match[1], 10) : Math.floor(maxTokens * 0.4);
    if (maxAffordable < 60) {
      const error = new Error(`HTTP 402: Saldo insuficiente (${maxAffordable} tokens disponíveis) para ${modelId}`);
      error.status = 402;
      throw error;
    }
    const affordableTokens = Math.max(60, maxAffordable - 15);
    console.warn(`[Auto-Adaptação 402] Ajustando tokens dinamicamente para ${affordableTokens} para ${modelId}...`);
    res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      signal: AbortSignal.timeout(20000),
      headers: {
        Authorization: `Bearer ${openRouterKey}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://midiastory-df6a0.web.app/",
        "X-Title": "Midiastore Failover Router",
      },
      body: JSON.stringify({
        model: modelId,
        max_tokens: affordableTokens,
        ...(responseFormatJson && !isFreeModel ? { response_format: { type: "json_object" } } : {}),
        ...(isReasoningModel ? { reasoning: { effort: reasoningEffort === "high" ? "medium" : reasoningEffort } } : {}),
        messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: prompt },
      ],
      }),
    });
  }

  if (!res.ok) {
    const errText = await res.text();
    const error = new Error(`HTTP ${res.status}: ${errText}`);
    error.status = res.status;
    throw error;
  }

  const data = await res.json();
  return data.choices?.[0]?.message?.content || "";
}

let cachedOpenRouterHasCredit = null;
async function hasOpenRouterPaidCredits() {
  if (cachedOpenRouterHasCredit !== null) return cachedOpenRouterHasCredit;
  if (!openRouterKey) {
    cachedOpenRouterHasCredit = false;
    return false;
  }
  try {
    const res = await fetch("https://openrouter.ai/api/v1/credits", {
      headers: { Authorization: `Bearer ${openRouterKey}` },
      signal: AbortSignal.timeout(2500),
    });
    if (res.ok) {
      const data = await res.json();
      const remaining = Number(data?.data?.total_credits || 0) - Number(data?.data?.total_usage || 0);
      cachedOpenRouterHasCredit = remaining > 0.005;
      return cachedOpenRouterHasCredit;
    }
  } catch {}
  cachedOpenRouterHasCredit = false;
  return false;
}

function callCodexLocal(prompt, systemPrompt = null) {
  let bin = codexPath;
  if (!fs.existsSync(bin)) {
    try {
      bin = execSync(process.platform === "win32" ? "where codex" : "which codex", { encoding: "utf8" }).trim().split("\n")[0];
    } catch (e) {
      bin = "";
    }
  }
  if (!bin || !fs.existsSync(bin)) {
    throw new Error(`Codex CLI não encontrado no ambiente (${process.platform})`);
  }
  console.log("⚡ Executando via OpenAI Codex CLI Local (Zero dependência de quota externa)...");
  const fullPrompt = buildCodexPromptWithPersona(prompt, systemPrompt);
  const res = spawnSync(
    bin,
    ["exec", "--cd", projectRoot, "--dangerously-bypass-approvals-and-sandbox", "-"],
    { input: fullPrompt, encoding: "utf8", timeout: 30000, maxBuffer: 10 * 1024 * 1024 }
  );
  if (res.error) throw res.error;
  if (res.status !== 0 && !res.stdout) {
    throw new Error(`Codex CLI saiu com código ${res.status}: ${(res.stderr || "").slice(0, 200)}`);
  }
  return (res.stdout || "").trim();
}

export async function executeWithFailover(prompt, options = {}) {
  const isCode = options.isCode || false;
  const maxTokens = options.maxTokens || (isCode ? 2500 : 1200);
  const responseFormatJson = Boolean(options.responseFormatJson);
  const forceProvider = options.forceProvider || null;
  const forceModel = options.forceModel || null;
  const sessionId = options.sessionId || null;
  const systemPrompt = await buildRodolfoBrainSystemPrompt({
    query: prompt,
    sessionId,
    limit: options.memoryLimit || 8,
  });

  console.log(`[FAILOVER ROUTER] Iniciando requisição (Modo: ${isCode ? "Código/Refatoração" : "Geral"}, maxTokens: ${maxTokens}, Provider: ${forceProvider || "auto"}, Model: ${forceModel || "auto"}, Memória Cognitiva: ativa)...`);

  async function finalizeResult(result) {
    if (options.autoLearn && result?.content) {
      if ((options.extractedFacts && options.extractedFacts.length > 0) || options.userCorrection) {
        await learnFromInteraction({
          question: prompt,
          answer: result.content,
          modelUsed: result.source,
          sessionId,
          userCorrection: options.userCorrection || null,
          extractedFacts: options.extractedFacts || [],
        });
      } else {
        await analyzeAndLearnTurn({
          question: prompt,
          answer: result.content,
          modelUsed: result.source,
          sessionId,
          origem: "failover_router_auto_learn",
        });
      }
    }
    return result;
  }

  // 1. Prioridade Nativa (Camada Central Agnóstica): Google AI Studio Direto priorizando Gemini 4 Carbon -> Hydrogen
  if (googleStudioKey && (!forceProvider || forceProvider === "google_ai_studio")) {
    const studioModels = forceModel ? [forceModel] : RODOLFO_BRAIN_MODEL_PRIORITY;
    for (const sm of studioModels) {
      const cbKey = `google_ai_studio:${sm}`;
      const gate = circuitBreaker.canRequest(cbKey);
      if (!gate.allowed && !forceModel) {
        console.log(`[Circuit Breaker OPEN] Pulando ${sm} (retry em ${gate.retryAfterSec}s)...`);
        continue;
      }
      console.log(`[Google AI Studio Direto] Testando modelo nativo: ${sm} (Thinking: ${isCode ? "HIGH" : "MEDIUM"})...`);
      try {
        const response = await callGoogleAIStudio(sm, prompt, {
          isCode,
          maxTokens,
          responseFormatJson,
          thinkingLevel: options.thinkingLevel || (isCode ? "HIGH" : "MEDIUM"),
          systemPrompt,
        });
        if (response) {
          circuitBreaker.recordSuccess(cbKey);
          console.log(`✅ Sucesso no Google AI Studio Direto (${sm})!`);
          return finalizeResult({ source: `google_ai_studio/${sm}`, content: response });
        }
      } catch (err) {
        circuitBreaker.recordFailure(cbKey, { status: err.status || null, message: err.message });
        console.warn(`⚠️ Google AI Studio (${sm}) falhou: ${err.message.slice(0, 140)}`);
      }
    }
  }

  // 2. Segunda Rota Gratuita Legal (somente se explicitamente solicitada — GitHub Models foi descontinuado em 2026-07-30)
  if (forceProvider === "github_models") {
    for (const gm of GITHUB_MODELS_CASCADE) {
      console.log(`[GitHub Models] Testando modelo gratuito: ${gm}...`);
      try {
        const response = await callGitHubModels(gm, prompt, maxTokens, responseFormatJson, systemPrompt);
        if (response) {
          console.log(`✅ Sucesso via GitHub Models (${gm})!`);
          return finalizeResult({ source: `github_models/${gm}`, content: response });
        }
      } catch (err) {
        console.warn(`⚠️ GitHub Models (${gm}) falhou: ${err.message.slice(0, 140)}`);
      }
    }
  }

  let lastError = null;

  if (openRouterKey && (!forceProvider || forceProvider === "openrouter")) {
    const paidAllowed = await hasOpenRouterPaidCredits();
    const baseCascade = paidAllowed
      ? MODEL_CASCADE
      : MODEL_CASCADE.filter((m) => m.id.endsWith(":free") || m.id === "openrouter/free");
    const orCascade =
      forceProvider === "openrouter" && forceModel
        ? [{ id: forceModel, name: forceModel }, ...baseCascade.filter((m) => m.id !== forceModel)]
        : baseCascade;
    for (let i = 0; i < orCascade.length; i++) {
      const model = orCascade[i];
      const cbKey = `openrouter:${model.id}`;
      const gate = circuitBreaker.canRequest(cbKey);
      if (!gate.allowed && !forceModel) {
        console.log(`[Circuit Breaker OPEN] Pulando ${model.id} (retry em ${gate.retryAfterSec}s)...`);
        continue;
      }
      console.log(`[Rota ${i + 1}/${orCascade.length}] Testando modelo: ${model.name} (${model.id})...`);
      try {
        const reasoningEffort = options.reasoningEffort || (isCode ? "high" : "medium");
        const response = await callOpenRouter(model.id, prompt, maxTokens, reasoningEffort, responseFormatJson, systemPrompt);
        if (response) {
          circuitBreaker.recordSuccess(cbKey);
          console.log(`✅ Sucesso na Rota ${i + 1} (${model.name})!`);
          return finalizeResult({ source: model.id, content: response });
        }
      } catch (err) {
        circuitBreaker.recordFailure(cbKey, { status: err.status || null, message: err.message });
        console.warn(`⚠️ Rota ${i + 1} (${model.name}) falhou: ${err.message.slice(0, 140)}`);
        lastError = err;
        continue;
      }
    }
  }

  if (forceProvider && forceProvider !== "codex_local") {
    throw new Error(`Provedor forçado '${forceProvider}' falhou ou está indisponível.`);
  }

  console.warn("⚠️ Todas as rotas de API remota esgotaram cota ou falharam. Acionando Fallback Local para o Codex CLI...");
  try {
    const localOutput = callCodexLocal(prompt, systemPrompt);
    console.log("✅ Sucesso no Fallback Local com Codex CLI!");
    return finalizeResult({ source: "local_codex", content: localOutput });
  } catch (codexErr) {
    console.error("❌ Falha tanto nas APIs remotas quanto no Codex local:", codexErr.message);
    throw new Error(`Falha total de failover. Último erro de API: ${lastError?.message}`);
  }
}

async function main() {
  const args = process.argv.slice(2);
  const isCode = args.includes("--code");
  const autoLearn = args.includes("--learn");
  const providerArg = args.find((a) => a.startsWith("--provider="));
  const modelArg = args.find((a) => a.startsWith("--model="));
  const sessionArg = args.find((a) => a.startsWith("--session="));
  const forceProvider = providerArg ? providerArg.split("=")[1] : null;
  const forceModel = modelArg ? modelArg.split("=")[1] : null;
  const sessionId = sessionArg ? sessionArg.split("=")[1] : null;
  const promptArgs = args.filter(
    (a) => a !== "--code" && a !== "--learn" && !a.startsWith("--provider=") && !a.startsWith("--model=") && !a.startsWith("--session=")
  );
  const prompt = promptArgs.join(" ").trim();

  if (!prompt) {
    console.log("Uso: node scripts/ai-failover-router.mjs [--code] [--learn] [--provider=google_ai_studio|github_models] [--model=nome] [--session=id] '<prompt>'");
    process.exit(1);
  }

  const result = await executeWithFailover(prompt, { isCode, autoLearn, forceProvider, forceModel, sessionId });
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
