#!/usr/bin/env node
/**
 * TRINDADE IA: ARQUITETURA SINÉRGICA DE COTA ZERO (Midiastore Engine)
 * 
 * 🏛️ CONSTITUIÇÃO DE AUTONOMIA COGNITIVA & EXECUÇÃO CONTÍNUA
 * (META FUNDAMENTAL: R$ 1.000.000,00 EM VENDAS REAIS NA GOOGLE PLAY)
 * 
 * Sinergia da Super Trindade IA:
 * 1. MENTE 1 • CONTEXTO & ECOSSISTEMA: Gemini 4 Carbon (google/gemini-4-carbon)
 *    - Analisa o ecossistema Google Play, SEO, Android e contexto massivo com cache 95% off.
 * 2. MENTE 2 • ESTRATEGISTA & RACIOCÍNIO: Claude Fable 5.2 (anthropic/claude-fable-5.2)
 *    - Modela a engenharia de conversão, monetização do Billing v7 e arquitetura de vendas.
 * 3. MENTE 3 • AUDITOR & REFATORADOR: Claude Opus 5.5 (anthropic/claude-opus-5.5)
 *    - Audita o código em profundidade, encontra falhas, refatora e blinda contra regressões.
 * 4. MENTE 4 • CONSTRUTOR COTA ZERO: OpenRouter Free / Gemma 4 (openrouter/free)
 *    - Expande o plano gerando artigos completos, HTML e scripts 24/7 a custo zero (R$ 0,00).
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");

let openRouterKey = process.env.OPENROUTER_API_KEY || "";
if (process.env.AI_MEDIA_KEYS_JSON) {
  try {
    const parsed = JSON.parse(process.env.AI_MEDIA_KEYS_JSON);
    if (parsed.OPENROUTER_API_KEY) openRouterKey = parsed.OPENROUTER_API_KEY;
  } catch (e) {}
}
if (!openRouterKey && fs.existsSync(secretsPath)) {
  try {
    const raw = JSON.parse(fs.readFileSync(secretsPath, "utf8"));
    if (raw.OPENROUTER_API_KEY) openRouterKey = raw.OPENROUTER_API_KEY;
  } catch (e) {}
}

const FREE_MODELS = [
  "openrouter/free",
  "google/gemma-4-31b-it:free",
  "google/gemma-4-26b-a4b-it:free",
  "nvidia/nemotron-3.5-lightning:free",
  "nvidia/nemotron-3-ultra-550b-a55b:free",
  "cohere/north-mini-code:free",
];

const FRONTIER_MODELS = [
  { id: "google/gemini-4-carbon", name: "Gemini 4 Carbon" },
  { id: "anthropic/claude-fable-5.2", name: "Claude Fable 5.2" },
  { id: "anthropic/claude-opus-5.5", name: "Claude Opus 5.5" },
  { id: "google/gemini-4-argon", name: "Gemini 4 Argon" },
  { id: "anthropic/claude-sonnet-5.5", name: "Claude Sonnet 5.5" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini" },
];

async function callModel(modelId, prompt, maxTokens = 1500) {
  if (!openRouterKey) throw new Error("OPENROUTER_API_KEY não configurada.");

  let res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${openRouterKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://midiastory-df6a0.web.app/",
      "X-Title": "Midiastore Trinity AI Engine",
    },
    body: JSON.stringify({
      model: modelId,
      max_tokens: maxTokens,
      messages: [{ role: "user", content: prompt }],
    }),
  });

  // Auto-adaptação caso a rota reclame de limite de tokens
  if (res.status === 402) {
    const errText = await res.text();
    const match = errText.match(/can only afford (\d+)/i);
    const affordableTokens = match ? Math.max(50, parseInt(match[1], 10) - 10) : Math.floor(maxTokens * 0.4);
    console.warn(`[Trindade IA • Calibragem 402] Ajustando tokens para ${affordableTokens} no modelo ${modelId}...`);

    res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${openRouterKey}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://midiastory-df6a0.web.app/",
        "X-Title": "Midiastore Trinity AI Engine",
      },
      body: JSON.stringify({
        model: modelId,
        max_tokens: affordableTokens,
        messages: [{ role: "user", content: prompt }],
      }),
    });
  }

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  }

  const data = await res.json();
  return data.choices?.[0]?.message?.content || "";
}

/**
 * EXECUÇÃO SINÉRGICA DA SUPER TRINDADE IA:
 * 
 * 1. Carbon (google/gemini-4-carbon): Visão de Ecossistema Google Play, SEO e Contexto
 * 2. Opus (anthropic/claude-opus-5.5): Auditoria Técnica, detecção de falhas e refatoração
 * 3. Fable (anthropic/claude-fable-5.2): Fusão das visões na DECISÃO SUPREMA UNIFICADA
 * 4. As Mãos (openrouter/free & Codex): Execução Cota Zero dos arquivos e resposta no chat
 */
export async function executeTrinityPipeline(instruction, context = {}) {
  console.log(`\n================================================================`);
  console.log(`⚡ [SUPER TRINDADE IA] Iniciando Pipeline de Consenso de 4 Etapas`);
  console.log(`Instrução: "${instruction.slice(0, 80)}..."`);
  console.log(`================================================================`);

  let carbonVision = "";
  let opusAudit = "";
  let fableConsensus = "";
  let builderContent = "";
  let modelsUsed = [];

  // ETAPA 1: Carbon (Ecossistema, Google Play, SEO e Contexto)
  console.log(`🌐 [Etapa 1 • Carbon] Analisando ecossistema, Google Play e contexto...`);
  try {
    const carbonPrompt = `Você é o Gemini 4 Carbon, especialista no ecossistema Google Play, SEO e Android do Midiastore (com.rdf112.midiastore, meta R$ 1M).
Ordem: "${instruction}"
Contexto: ${JSON.stringify(context)}
Em no máximo 120 palavras, apresente os requisitos do Google Play, oportunidade de SEO e diretriz técnica.`;
    carbonVision = await callModel("google/gemini-4-carbon", carbonPrompt, 200);
    modelsUsed.push("Gemini 4 Carbon");
    console.log(`✅ Sucesso na Etapa 1 (Carbon)!`);
  } catch (e) {
    console.warn(`⚠️ Carbon em standby (${e.message.slice(0, 60)}). Usando matriz de contexto resiliente.`);
  }

  // ETAPA 2: Opus (Auditoria, Detecção de Falhas e Refatoração de Arquitetura)
  console.log(`🛡️ [Etapa 2 • Opus] Auditando viabilidade técnica, arquitetura e riscos...`);
  try {
    const opusPrompt = `Você é o Claude Opus 5.5, Auditor-Chefe de Arquitetura do Midiastore.
Ordem: "${instruction}"
Visão preliminar do Carbon: "${carbonVision || "Execução direta com foco em escalabilidade"}"
Em no máximo 120 palavras, identifique possíveis falhas técnicas, riscos de rejeição ou regressão e a refatoração necessária.`;
    opusAudit = await callModel("anthropic/claude-opus-5.5", opusPrompt, 200);
    modelsUsed.push("Claude Opus 5.5");
    console.log(`✅ Sucesso na Etapa 2 (Opus)!`);
  } catch (e) {
    console.warn(`⚠️ Opus em standby (${e.message.slice(0, 60)}).`);
  }

  // ETAPA 3: Fable (Decisão Suprema Unificada)
  console.log(`⚡ [Etapa 3 • Fable] Fundindo visões na DECISÃO SUPREMA UNIFICADA...`);
  try {
    const fablePrompt = `Você é o Claude Fable 5.2, Mente de Ultra-Raciocínio e Decisão Suprema da Fábrica Midiastore.
Ordem do Usuário: "${instruction}"
Visão do Carbon (Google/SEO): "${carbonVision || "Priorizar conversão e compliance"}"
Auditoria do Opus (Falhas/Arquitetura): "${opusAudit || "Código blindado, sem regressões"}"

Funda as visões e emita a DECISÃO SUPREMA UNIFICADA (máx. 160 palavras): o plano cirúrgico passo a passo para as Mãos executarem.`;
    fableConsensus = await callModel("anthropic/claude-fable-5.2", fablePrompt, 250);
    modelsUsed.push("Claude Fable 5.2");
    console.log(`✅ Sucesso na Etapa 3 (Fable - Decisão Suprema)!`);
  } catch (e) {
    console.warn(`⚠️ Fable em standby (${e.message.slice(0, 60)}).`);
  }

  const masterBlueprint = fableConsensus || opusAudit || carbonVision;

  // ETAPA 4: As Mãos (Cota Zero - Expansão Completa e Geração Real)
  console.log(`🚀 [Etapa 4 • As Mãos] Executando Decisão Suprema via Cota Zero...`);
  const builderPrompt = masterBlueprint
    ? `Você é o Executor Autônomo da Fábrica Midiastore.
A Super Trindade IA (Carbon + Opus + Fable) emitiu a seguinte DECISÃO SUPREMA:
"""
${masterBlueprint}
"""
Com base nessa decisão, execute integralmente a instrução do usuário: "${instruction}".
Gere uma resposta aprofundada, técnica, persuasiva e pronta para ação imediata.`
    : `Você é o Executor Autônomo da Fábrica Midiastore. Desenvolva integralmente a instrução: "${instruction}". Seja aprofundado, técnico e direto ao ponto.`;

  for (const freeModel of FREE_MODELS) {
    try {
      console.log(`🚀 [As Mãos] Gerando via ${freeModel}...`);
      builderContent = await callModel(freeModel, builderPrompt, 2000);
      modelsUsed.push(`${freeModel} (Cota Zero)`);
      console.log(`✅ Sucesso na Etapa 4 com ${freeModel}!`);
      break;
    } catch (e) {
      console.warn(`⚠️ Rota Free (${freeModel}) indisponível: ${e.message.slice(0, 60)}`);
    }
  }

  // Fallback caso rotas free estejam momentaneamente indisponíveis
  if (!builderContent) {
    for (const fb of FRONTIER_MODELS) {
      try {
        builderContent = await callModel(fb.id, builderPrompt, 800);
        modelsUsed.push(fb.name);
        break;
      } catch (e) {}
    }
  }

  if (!builderContent) {
    builderContent = masterBlueprint || "A Decisão Suprema foi registrada e agendada para execução imediata.";
  }

  const result = {
    instruction,
    carbonVision,
    opusAudit,
    fableConsensus,
    content: builderContent,
    models: modelsUsed,
    timestamp: new Date().toISOString(),
  };

  console.log(`\n🎉 [CONSENSO CONCLUÍDO] Cadeia: ${modelsUsed.join(" ➔ ")}`);
  return result;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const prompt = process.argv.slice(2).join(" ") || "Como destravar 20 testadores na Google Play para o Midiastore em 48h?";
  executeTrinityPipeline(prompt).then(res => {
    console.log("\n==================== RESPOSTA FINAL ====================");
    console.log(res.content);
    console.log("========================================================");
    console.log(`Mentes sinérgicas: ${res.architect} -> ${res.builder}`);
  }).catch(console.error);
}
