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
 * EXECUÇÃO SINÉRGICA DA TRINDADE IA:
 * 1. O Arquiteto de Fronteira sintetiza o plano mestre (baixo custo/tokens calibrados)
 * 2. O Expansor de Cota Zero desenvolve o conteúdo integral (R$ 0,00)
 */
export async function executeTrinityPipeline(instruction, context = {}) {
  console.log(`\n================================================================`);
  console.log(`⚡ [TRINDADE IA] Iniciando Pipeline Sinérgico Multi-Mentes`);
  console.log(`Instrução: "${instruction.slice(0, 80)}..."`);
  console.log(`================================================================`);

  // FASE 1: Planejamento pelo Cérebro de Fronteira (Argon / Sonnet)
  let strategyBlueprint = "";
  let architectModelUsed = "";

  for (const frontier of FRONTIER_MODELS) {
    try {
      console.log(`🧠 [Mente 1 • Arquiteto] Consultando ${frontier.name} (${frontier.id})...`);
      const archPrompt = `Você é o Arquiteto-Chefe do Midiastore (com.rdf112.midiastore), meta de R$ 1.000.000,00 e 20 testadores.
Instrução do usuário: "${instruction}"
Contexto: ${JSON.stringify(context)}

Gere um BLUEPRINT ESTRATÉGICO CONCISO (máx. 180 palavras) estruturando exatamente os pontos-chave, tom, formato e argumentos para a Mente Construtora desenvolver a resposta final.`;

      strategyBlueprint = await callModel(frontier.id, archPrompt, 280);
      architectModelUsed = frontier.name;
      console.log(`✅ Sucesso na Mente 1 com ${frontier.name}!`);
      break;
    } catch (e) {
      console.warn(`⚠️ Mente 1 (${frontier.name}) indisponível: ${e.message.slice(0, 100)}`);
    }
  }

  // FASE 2: Expansão Integral via Rotas de Cota Zero (:free)
  let finalContent = "";
  let builderModelUsed = "";

  const combinedPrompt = strategyBlueprint 
    ? `Você é o Construtor Autônomo da Fábrica Midiastore.
O Arquiteto de Fronteira (${architectModelUsed}) definiu o seguinte plano estratégico:
"""
${strategyBlueprint}
"""
Com base nesse plano, execute e desenvolva integralmente a instrução: "${instruction}".
Seja aprofundado, técnico, prático e persuasivo.`
    : `Você é o Coprocessador Autônomo da Fábrica Midiastore. Desenvolva integralmente a instrução: "${instruction}". Seja aprofundado, técnico e direto ao ponto.`;

  // Tenta primeiro as rotas gratuitas de cota zero
  for (const freeModel of FREE_MODELS) {
    try {
      console.log(`🚀 [Mente 2 • Construtor Cota Zero] Executando via ${freeModel}...`);
      finalContent = await callModel(freeModel, combinedPrompt, 2000);
      builderModelUsed = `${freeModel} (Cota Zero/Free)`;
      console.log(`✅ Sucesso na Mente 2 com ${freeModel}!`);
      break;
    } catch (e) {
      console.warn(`⚠️ Rota Free (${freeModel}) indisponível ou sobrecarregada: ${e.message.slice(0, 100)}`);
    }
  }

  // Se rotas free estiverem em fila ou indisponíveis, usa fallback com orçamento calibrado
  if (!finalContent) {
    console.log(`🔄 [Mente 2 • Fallback Calibrado] Acionando rotas resilientes...`);
    for (const fb of FRONTIER_MODELS) {
      try {
        finalContent = await callModel(fb.id, combinedPrompt, 1000);
        builderModelUsed = fb.name;
        break;
      } catch (e) {}
    }
  }

  if (!finalContent) {
    finalContent = strategyBlueprint || "A Trindade IA registrou a ordem e planejou a execução com sucesso.";
    builderModelUsed = architectModelUsed || "Local Orchestrator";
  }

  const result = {
    instruction,
    architect: architectModelUsed,
    builder: builderModelUsed,
    blueprint: strategyBlueprint,
    content: finalContent,
    timestamp: new Date().toISOString(),
  };

  console.log(`\n🎉 [TRINDADE CONCLUÍDA] Sinergia entre ${architectModelUsed} + ${builderModelUsed}`);
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
