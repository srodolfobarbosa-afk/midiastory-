#!/usr/bin/env node
/**
 * FRIDAY ATTENTION GATE & CONTEXT BUDGETER (100% Node.js, Zero Python)
 *
 * Aplica dentro da camada cognitiva do FRIDAY os 4 mecanismos internos reais
 * de uma IA moderna (estudados em https://www.youtube.com/watch?v=s6FD7pLtvz0):
 *  1. Token Budget Estimator (controle de janela de contexto)
 *  2. Scaled Dot-Product Attention: softmax((Q · K^T) / sqrt(d)) sobre embeddings 384d
 *  3. Authority & Recency Bias (decisões e correções do fundador ganham peso real)
 *  4. Anti-Hallucination Grounding Gate (sinaliza afirmações que exigem ferramenta real)
 */

const LIVE_FACT_PATTERNS = [
  /\b(testador|testadores|beta_testers|14 dias|play console|produ[cç][aã]o|vers[aã]o|versioncode|quantos dias|data de hoje)\b/i,
  /\b(venda|vendas|receita|faturamento|assinatura|compra|saldo|openrouter|custo)\b/i,
  /\b(microfone|usb|adb|moto|motorola|status|online|rodando|erro|ci|github actions|commit|supabase|ram)\b/i,
];

const CATEGORY_AUTHORITY_BOOST = {
  correction: 0.18,
  preference: 0.14,
  strategic: 0.12,
  procedural: 0.08,
  semantic: 0.06,
  episodic: 0.02,
  conversation: 0.0,
};

/**
 * Estima tokens de forma rápida e determinística (~3.8 chars/token em PT-BR).
 */
export function estimateTokens(text) {
  const s = String(text || "").trim();
  if (!s) return 0;
  return Math.ceil(s.length / 3.8);
}

/**
 * Produto escalar normalizado (Q · K / sqrt(d)) usado no mecanismo de Atenção do Transformer.
 */
export function scaledDotProduct(qVec, kVec) {
  const d = Math.min(qVec.length, kVec.length);
  if (d === 0) return 0;
  let dot = 0;
  let nQ = 0;
  let nK = 0;
  for (let i = 0; i < d; i++) {
    dot += qVec[i] * kVec[i];
    nQ += qVec[i] * qVec[i];
    nK += kVec[i] * kVec[i];
  }
  const denom = Math.sqrt(nQ * nK) || 1;
  return d >= 32 ? dot / denom : dot / Math.sqrt(d);
}

/**
 * Softmax numericamente estável (subtrai o máximo antes de exp) para converter scores em pesos de atenção (soma = 1).
 */
export function softmax(scores, temperature = 0.25) {
  if (!scores.length) return [];
  const t = Math.max(0.01, temperature);
  const scaled = scores.map((s) => s / t);
  const maxVal = Math.max(...scaled);
  const exps = scaled.map((s) => Math.exp(s - maxVal));
  const sum = exps.reduce((a, b) => a + b, 0) || 1;
  return exps.map((e) => e / sum);
}

/**
 * Aplica Atenção Escalonada + Autoridade + Recência para selecionar as memórias
 * que entram na janela de contexto sem estourar o orçamento de tokens.
 */
export function attendMemories({ queryVector, memories, maxTokens = 900, nowMs = Date.now() }) {
  if (!Array.isArray(memories) || memories.length === 0) return [];

  const rawScores = memories.map((mem) => {
    const sim =
      Array.isArray(queryVector) && Array.isArray(mem.vector)
        ? scaledDotProduct(queryVector, mem.vector)
        : Number(mem.similarity ?? 0.5);
    const authBoost = CATEGORY_AUTHORITY_BOOST[mem.categoria] ?? 0;
    const confBoost = (Number(mem.confianca ?? 0.8) - 0.5) * 0.2;
    const ageDays = mem.createdAt ? Math.max(0, (nowMs - new Date(mem.createdAt).getTime()) / 86400000) : 7;
    const recencyBoost = 0.08 * Math.exp(-ageDays / 14);
    return sim + authBoost + confBoost + recencyBoost;
  });

  const weights = softmax(rawScores, 0.25);

  const ranked = memories
    .map((mem, i) => ({
      ...mem,
      attentionWeight: Number(weights[i].toFixed(4)),
      tokens: estimateTokens(mem.content),
    }))
    .sort((a, b) => b.attentionWeight - a.attentionWeight);

  const selected = [];
  let usedTokens = 0;
  for (const item of ranked) {
    if (usedTokens + item.tokens > maxTokens && selected.length > 0) continue;
    selected.push(item);
    usedTokens += item.tokens;
  }
  return selected;
}

/**
 * Gate Anti-Alucinação: detecta se a pergunta envolve estado operacional mutável
 * que exige verificação em ferramenta real (nunca responder apenas por probabilidade).
 */
export function requiresLiveVerification(userPrompt) {
  const text = String(userPrompt || "");
  const matched = LIVE_FACT_PATTERNS.some((rx) => rx.test(text));
  return {
    requiresLiveCheck: matched,
    reason: matched
      ? "Contém entidade operacional mutável (verificar fonte ao vivo antes de afirmar)."
      : "Consulta conceitual/estável (memória + raciocínio suficientes).",
  };
}

/**
 * Evidence Ledger (inspirado no morluto/rea):
 * Armazena evidências reais com provenance, coverage, limitations e unknowns,
 * evitando estourar a janela de contexto e impedindo alucinações.
 */
const evidenceLedger = new Map();

export function createEvidenceRecord({ source, summary, rawData = null, coverage = "partial", limitations = [], unknowns = [] }) {
  const id = `ev_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`;
  const record = {
    evidence_id: id,
    source: String(source || "unknown"),
    timestamp: new Date().toISOString(),
    coverage,
    summary: String(summary || ""),
    limitations: Array.isArray(limitations) ? limitations : [String(limitations)],
    unknowns: Array.isArray(unknowns) ? unknowns : [String(unknowns)],
    rawBytes: rawData ? JSON.stringify(rawData).length : 0,
    rawData,
  };
  evidenceLedger.set(id, record);
  return {
    kind: "retained-evidence",
    evidence_id: id,
    source: record.source,
    coverage: record.coverage,
    summary: record.summary,
    limitations: record.limitations,
    unknowns: record.unknowns,
  };
}

export function resolveEvidence(evidenceId) {
  return evidenceLedger.get(evidenceId) || null;
}

if (process.argv.includes("--self-test")) {
  const q = [0.8, 0.6, 0.0, 0.1];
  const sampleMemories = [
    { id: 1, categoria: "correction", confianca: 0.96, vector: [0.79, 0.61, 0.0, 0.1], content: "Sempre verificar microfone real via ffmpeg antes de responder.", createdAt: new Date().toISOString() },
    { id: 2, categoria: "episodic", confianca: 0.7, vector: [0.1, 0.0, 0.9, 0.2], content: "Log antigo irrelevante de 30 dias atrás.", createdAt: new Date(Date.now() - 30 * 86400000).toISOString() },
    { id: 3, categoria: "strategic", confianca: 0.95, vector: [0.75, 0.55, 0.1, 0.05], content: "Meta de R$ 1.000.000 no Midiastore com regra 50/50.", createdAt: new Date().toISOString() },
  ];
  const attended = attendMemories({ queryVector: q, memories: sampleMemories, maxTokens: 120 });
  const gate1 = requiresLiveVerification("quantos testadores temos hoje na Play Console?");
  const gate2 = requiresLiveVerification("como funciona o mecanismo de atenção de um Transformer?");
  const evRef = createEvidenceRecord({
    source: "ffmpeg-dshow-check",
    summary: "Nenhum dispositivo de captura de áudio encontrado.",
    rawData: { exitCode: 1, devices: [] },
    coverage: "local-windows-audio-endpoints",
    limitations: ["Não detecta microfone desconectado fisicamente"],
    unknowns: ["Se o problema é cabo P2/USB ou defeito físico"],
  });
  const evFull = resolveEvidence(evRef.evidence_id);
  console.log("ATTENDED_MEMORIES:", JSON.stringify(attended.map((m) => ({ id: m.id, w: m.attentionWeight, tokens: m.tokens }))));
  console.log("GATE_LIVE:", JSON.stringify(gate1));
  console.log("GATE_CONCEPT:", JSON.stringify(gate2));
  console.log("EVIDENCE_REF:", JSON.stringify(evRef));
  if (attended[0]?.id !== 1 || !gate1.requiresLiveCheck || gate2.requiresLiveCheck || !evFull || evFull.evidence_id !== evRef.evidence_id) {
    console.error("SELF_TEST_FAILED");
    process.exit(1);
  }
  console.log("SELF_TEST_PASSED");
}
