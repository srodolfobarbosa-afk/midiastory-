#!/usr/bin/env node
/**
 * FRIDAY Triage Engine — Motor Automático de Decomposição, Peso e Roteamento de Subagentes
 *
 * Roda em 2 modos:
 *   1. Hook PreInvocation do Antigravity CLI (`node ../scripts/friday-triage-engine.mjs --hook-pre`)
 *      Intercepta toda mensagem do fundador, decompõe em itens individuais, mede o peso/complexidade
 *      de cada item e injeta um Checklist Obrigatório de Despacho antes da IA raciocinar.
 *   2. CLI de teste (`node scripts/friday-triage-engine.mjs --test "<mensagem>"`)
 *
 * Custo: R$ 0,00 | Latência: <15ms | Zero Python | Zero RAM residente.
 */

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const STATE_FILE = path.join(os.tmpdir(), "friday-triage-hook-state.json");

function readStdin() {
  return new Promise((resolve) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      data += chunk;
    });
    process.stdin.on("end", () => resolve(data));
    process.stdin.on("error", () => resolve(data));
  });
}

function loadState() {
  try {
    if (fs.existsSync(STATE_FILE)) {
      return JSON.parse(fs.readFileSync(STATE_FILE, "utf8"));
    }
  } catch {}
  return { byConv: {} };
}

function saveState(state) {
  try {
    const trimmed = Object.fromEntries(Object.entries(state.byConv || {}).slice(-30));
    fs.writeFileSync(STATE_FILE, JSON.stringify({ byConv: trimmed }), "utf8");
  } catch {}
}

function stripUserRequestTags(text) {
  const s = String(text || "");
  const match = s.match(/<USER_REQUEST>\s*([\s\S]*?)\s*<\/USER_REQUEST>/i);
  return (match ? match[1] : s).trim();
}

function splitIntoAtomicItems(rawText) {
  const cleaned = String(rawText || "")
    .replace(/\r\n/g, "\n")
    .replace(/\s+/g, " ")
    .trim();

  if (!cleaned) return [];

  // Divide por pontuação forte, quebras de linha, conectivos de novo pedido e vírgula/"e" + verbo de ação
  const ACTION_VERBS = "(?:cri[ae]|fa[çz][ae]?|adicion[ae]|instal[ae]|corrig[ae]|melhor[ae]|pesquis[ae]|analis[ae]|audit[ae]|constr[oó]i|constru[ae]|rod[ae]|test[ae]|public[ae]|vend[ae]|verifiqu[ae]|chequ[ae]|atualiz[ae]|sub[ae]|coloc[ae]|coloque|bot[ae]|mand[ae]|prop[oõ]e|estud[ae]|implement[ae])\\w*";
  const splitRegex = new RegExp(
    `(?<=[.!?])\\s+|\\n+|(?:[,;]?\\s+(?:outra coisa|e tamb[eé]m|al[eé]m disso|al[eé]m de|e eu queria|tamb[eé]m quero|segundo ponto|terceiro ponto|por [uú]ltimo|agora faz|e outra)[,:]?\\s+)|(?:(?:,|\\s+e)\\s+(?=${ACTION_VERBS}\\b))`,
    "i"
  );

  const roughParts = cleaned
    .split(splitRegex)
    .map((p) => p.replace(/^[,;:\-\s]+/, "").trim())
    .filter((p) => p.length >= 12);

  if (roughParts.length === 0) return [cleaned];

  // Agrupa fragmentos curtos de contexto (<35 chars sem pergunta/verbo forte) no item anterior ou próximo
  const merged = [];
  for (const part of roughParts) {
    const isShortFiller =
      part.length < 35 &&
      !part.includes("?") &&
      !/\b(cri[ae]|fa[çz]|adicion|instal|corrig|melhor|pesquis|analis|audit|subagent|constr|rod[ae]|test[ae]|public|vend)\w*/i.test(part);

    if (isShortFiller && merged.length > 0) {
      merged[merged.length - 1] = `${merged[merged.length - 1]} ${part}`.trim();
    } else {
      merged.push(part);
    }
  }

  return merged.slice(0, 12);
}

function detectPillar(text) {
  const t = text.toLowerCase();
  const isMidiastore =
    /\b(midiastore|midiastory|apk|aab|kotlin|gradle|play console|play store|testador|billing|paywall|mediastore|moto g84|adb|limpador|duplicata|venda|faturar)\b/i.test(t);
  const isFriday =
    /\b(voc[eê] mesm|subagent|sub-agent|friday|agi|superintelig|voz|avatar|hud|quadro|cota|mem[oó]ria|hook|mcp|ferramenta|construtor|evolu[ií])\b/i.test(t);

  if (isMidiastore && isFriday) return "HÍBRIDO (FRIDAY + MIDIASTORE)";
  if (isFriday) return "PILAR 2: FRIDAY (IA Construtora)";
  if (isMidiastore) return "PILAR 1: MIDIASTORE (Produto Comercial)";
  return "GERAL / ESTRATÉGIA";
}

function scoreItem(itemText) {
  const t = itemText.toLowerCase();
  let score = 1;
  const reasons = [];

  // Sinais de peso CRÍTICO / ALTO
  if (/\b(arquitetur|refator|sistema|motor|engine|subagent|swarm|orquestr|billing|play console|aab|release|produ[çc][aã]o|kotlin|gradle|seguran[çc]a|supabase|edge function|agi|superintelig)\b/i.test(t)) {
    score += 4;
    reasons.push("engenharia/arquitetura");
  }
  if (/\b(cri[ae]|constr[oó]i|constru|desenvolv|implement|automatiz|integr|instal|adicion)\w*/i.test(t)) {
    score += 3;
    reasons.push("construção real");
  }
  if (/\b(pesquis|estud|audit|varredur|analis|compar|concorrent|mercado|estrat[eé]gi|fora da caixa|prop[oõ]e|melhor)\w*/i.test(t)) {
    score += 2;
    reasons.push("pesquisa/estratégia");
  }
  if (/\b(imagem|avatar|banner|screenshot|mockup|visual|design|tela|ui|ux)\b/i.test(t)) {
    score += 2;
    reasons.push("mídia/visual");
  }
  if (/\b(erro|bug|falh|quebr|cort|n[aã]o funcion|trav|corrig)\w*/i.test(t)) {
    score += 2;
    reasons.push("depuração");
  }
  if (itemText.length > 140) {
    score += 1;
    reasons.push("alta densidade");
  }

  let weight = "BAIXO";
  let route = "Execução direta imediata (ou subagente flash_lite)";

  if (/\b(imagem|avatar|banner|ilustr|desenh|frame)\b/i.test(t)) {
    weight = score >= 5 ? "ALTO" : "MÉDIO";
    route = "Subagente `image-generator` (em paralelo se múltiplas artes)";
  } else if (score >= 7) {
    weight = "CRÍTICO";
    route = "Subagente `pro`/`inherit` ou `swarm` multi-agente + verificação real";
  } else if (score >= 5) {
    weight = "ALTO";
    route = "Subagente especialista (`self` com `inherit`/`pro` ou `codex.exe`) + prova real";
  } else if (score >= 3) {
    weight = "MÉDIO";
    route = "Subagente `research` / `flash` em paralelo (ou execução cirúrgica direta)";
  }

  return {
    text: itemText,
    score,
    weight,
    route,
    reasons: reasons.length ? reasons.join(", ") : "consulta/confirmação",
  };
}

export function analyzePrompt(rawText) {
  const clean = stripUserRequestTags(rawText);
  const items = splitIntoAtomicItems(clean).map(scoreItem);
  const pillar = detectPillar(clean);

  const highCount = items.filter((i) => i.weight === "ALTO" || i.weight === "CRÍTICO").length;
  const medCount = items.filter((i) => i.weight === "MÉDIO").length;

  let dispatchMode = "DIRETO";
  if (highCount >= 2 || items.length >= 4) {
    dispatchMode = "PARALELO / SWARM (múltiplas frentes ou alta densidade)";
  } else if (highCount === 1 && medCount >= 1) {
    dispatchMode = "HÍBRIDO (núcleo forte + especialista paralelo)";
  } else if (highCount === 1) {
    dispatchMode = "ENGENHARIA PROFUNDA (foco total + validação real)";
  }

  return {
    pillar,
    totalItems: items.length,
    highCount,
    medCount,
    dispatchMode,
    items,
  };
}

function formatTriageBlock(analysis) {
  const lines = [
    `[TRIAGEM AUTOMÁTICA FRIDAY — ZERO ITEM IGNORADO]`,
    `Pilar: ${analysis.pillar} | Itens detectados: ${analysis.totalItems} | Modo: ${analysis.dispatchMode}`,
    `Checklist obrigatório (responda/execute 100% dos itens abaixo, respeitando o peso):`,
  ];

  analysis.items.forEach((item, idx) => {
    const snippet = item.text.length > 130 ? `${item.text.slice(0, 127)}...` : item.text;
    lines.push(
      `  ${idx + 1}. [${item.weight} | p=${item.score}] ${snippet}\n     -> Rota: ${item.route} (${item.reasons})`
    );
  });

  return lines.join("\n");
}

async function runHook() {
  const raw = await readStdin();
  let payload = {};
  try {
    payload = raw ? JSON.parse(raw) : {};
  } catch {}

  const convId = String(payload.conversationId || "default");
  const cleanPrompt = stripUserRequestTags(payload.lastUserInput || "");

  // Ignora comandos curtos simples (/compact, "ok", "sim")
  if (!cleanPrompt || cleanPrompt.length < 25 || cleanPrompt.startsWith("/")) {
    process.stdout.write("{}");
    return;
  }

  const hash = crypto.createHash("sha256").update(cleanPrompt).digest("hex").slice(0, 16);
  const state = loadState();
  if (state.byConv[convId] === hash) {
    process.stdout.write("{}");
    return;
  }
  state.byConv[convId] = hash;
  saveState(state);

  const analysis = analyzePrompt(cleanPrompt);
  // Só injeta quando há >= 2 itens ou pelo menos 1 item de peso MÉDIO/ALTO/CRÍTICO
  if (analysis.totalItems < 2 && analysis.highCount === 0 && analysis.medCount === 0) {
    process.stdout.write("{}");
    return;
  }

  const output = {
    injectSteps: [
      {
        systemMessage: {
          systemMessage: formatTriageBlock(analysis),
        },
      },
    ],
  };

  process.stdout.write(JSON.stringify(output));
}

async function main() {
  const args = process.argv.slice(2);
  if (args.includes("--hook-pre")) {
    await runHook();
    return;
  }

  const testIdx = args.indexOf("--test");
  if (testIdx !== -1) {
    const sample = args.slice(testIdx + 1).join(" ").trim();
    const analysis = analyzePrompt(sample);
    console.log(formatTriageBlock(analysis));
    return;
  }

  console.log("Uso: node scripts/friday-triage-engine.mjs --test \"<mensagem>\" | --hook-pre");
}

main().catch(() => {
  if (process.argv.includes("--hook-pre")) {
    process.stdout.write("{}");
  }
});
