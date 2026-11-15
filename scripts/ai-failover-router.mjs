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
import {
  RODOLFO_BRAIN_SYSTEM_PROMPT,
  RODOLFO_BRAIN_MODEL_PRIORITY,
  buildCodexPromptWithPersona,
  buildRodolfoBrainSystemPrompt,
} from "./lib/rodolfo-brain-persona.mjs";
import { learnFromInteraction } from "./lib/cognitive-memory.mjs";

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

async function callGoogleAIStudio(modelName, prompt, options = {}) {
  if (!googleStudioKey) throw new Error("Chave GOOGLE_AI_STUDIO_API_KEY não encontrada.");
  const isGemini4Thinking = modelName.includes("gemini-4-hydrogen") || modelName.includes("gemini-4-carbon") || modelName.includes("gemini-4-argon");
  const thinkingLevel = options.thinkingLevel || (options.isCode ? "HIGH" : "MEDIUM");

  const generationConfig = {
    ...(options.maxTokens ? { maxOutputTokens: options.maxTokens } : {}),
    ...(options.responseFormatJson ? { responseMimeType: "application/json" } : {}),
    ...(isGemini4Thinking ? { thinkingConfig: { thinkingLevel } } : {}),
  };

  const bodyPayload = {
    systemInstruction: { parts: [{ text: options.systemPrompt || RODOLFO_BRAIN_SYSTEM_PROMPT }] },
    contents: [{ parts: [{ text: prompt }] }],
    ...(Object.keys(generationConfig).length > 0 ? { generationConfig } : {}),
  };

  const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${googleStudioKey}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    signal: AbortSignal.timeout(options.timeoutMs || 25000),
    body: JSON.stringify(bodyPayload),
  });
  if (!res.ok) {
    const errText = await res.text();
    const error = new Error(`HTTP ${res.status}: ${errText}`);
    error.status = res.status;
    throw error;
  }
  const data = await res.json();
  const parts = data.candidates?.[0]?.content?.parts || [];
  return parts.map((p) => p.text || "").join("").trim();
}

const MODEL_CASCADE = [
  // 1. Modelos Frontier com Raciocínio (Tentativa primária com auto-adaptação e BYOK)
  { id: "google/gemini-4-carbon", name: "Gemini 4 Carbon (1M Context + High Reasoning)" },
  { id: "google/gemini-4-argon", name: "Gemini 4 Argon" },
  { id: "google/gemini-3.8-flash", name: "Gemini 3.8 Flash" },
  { id: "google/gemini-3.1-flash-lite", name: "Gemini 3.1 Flash-Lite" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini" },

  // 2. Modelos Gratuitos Validados no OpenRouter (Custo Zero, Sem 402, Imunes ao Bloqueio de 156h Google)
  { id: "nvidia/nemotron-3.5-lightning:free", name: "NVIDIA Nemotron 3.5 Lightning (Free)" },
  { id: "baidu/ernie-5.5-flash-preview:free", name: "Baidu ERNIE 5.5 Flash (Free)" },
  { id: "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", name: "NVIDIA Nemotron 3 Nano Omni (Free)" },
  { id: "google/gemma-4-31b-it:free", name: "Google Gemma 4 31B (Free)" },
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

function callCodexLocal(prompt) {
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
  const cleanPrompt = buildCodexPromptWithPersona(prompt).replace(/"/g, '\\"');
  return execSync(
    `"${bin}" exec --cd "${projectRoot}" --dangerously-bypass-approvals-and-sandbox "${cleanPrompt}"`,
    { encoding: "utf8", maxBuffer: 10 * 1024 * 1024 }
  );
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
      await learnFromInteraction({
        question: prompt,
        answer: result.content,
        modelUsed: result.source,
        sessionId,
        extractedFacts: options.extractedFacts || [],
      });
    }
    return result;
  }

  // 1. Prioridade Nativa (Camada Central Agnóstica): Google AI Studio Direto priorizando Gemini 4 Carbon -> Hydrogen
  if (googleStudioKey && (!forceProvider || forceProvider === "google_ai_studio")) {
    const studioModels = forceModel ? [forceModel] : RODOLFO_BRAIN_MODEL_PRIORITY;
    for (const sm of studioModels) {
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
          console.log(`✅ Sucesso no Google AI Studio Direto (${sm})!`);
          return finalizeResult({ source: `google_ai_studio/${sm}`, content: response });
        }
      } catch (err) {
        console.warn(`⚠️ Google AI Studio (${sm}) falhou: ${err.message.slice(0, 140)}`);
      }
    }
  }

  // 2. Segunda Rota Gratuita Legal: GitHub Models (GPT-4o / GPT-4o-mini / DeepSeek-R1 via conta GitHub)
  if (!forceProvider || forceProvider === "github_models") {
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
    for (let i = 0; i < MODEL_CASCADE.length; i++) {
      const model = MODEL_CASCADE[i];
      console.log(`[Rota ${i + 1}/${MODEL_CASCADE.length}] Testando modelo: ${model.name} (${model.id})...`);
      try {
        const reasoningEffort = options.reasoningEffort || (isCode ? "high" : "medium");
        const response = await callOpenRouter(model.id, prompt, maxTokens, reasoningEffort, responseFormatJson, systemPrompt);
        if (response) {
          console.log(`✅ Sucesso na Rota ${i + 1} (${model.name})!`);
          return finalizeResult({ source: model.id, content: response });
        }
      } catch (err) {
        console.warn(`⚠️ Rota ${i + 1} (${model.name}) falhou: ${err.message.slice(0, 140)}`);
        lastError = err;
        continue;
      }
    }
  }

  if (forceProvider) {
    throw new Error(`Provedor forçado '${forceProvider}' falhou ou está indisponível.`);
  }

  console.warn("⚠️ Todas as rotas de API remota esgotaram cota ou falharam. Acionando Fallback Local para o Codex CLI...");
  try {
    const localOutput = callCodexLocal(prompt);
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
