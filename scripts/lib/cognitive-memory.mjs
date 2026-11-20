import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..", "..");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");

export const EMBEDDING_DIMS = 384;
export const NEURAL_EMBEDDING_MODEL = "gemini-embedding-001";
export const DETERMINISTIC_EMBEDDING_MODEL = "deterministic_ngram_384";

export const VALID_MEMORY_CATEGORIES = new Set([
  "episodic",
  "semantic",
  "procedural",
  "strategic",
  "preference",
  "correction",
  "conversation",
]);

const SPECULATIVE_REGEX =
  /\b(talvez|acho que|suponho|provavelmente|chute|não verificado|sem confirmação|hipótese|pode ser que|acredito que|quem sabe)\b/i;

const TRIVIAL_TURN_REGEX =
  /^(oi|ol[áa]|e a[íi]|tudo bem\??|ok|blz|beleza|valeu|obrigado|obrigada|show|top|sim|n[ãa]o)\s*[!?.]*$/i;

function loadAiKeys() {
  const keys = {
    googleStudioKey: process.env.GOOGLE_AI_STUDIO_API_KEY || process.env.GOOGLE_AQ_API_KEY || "",
    openRouterKey: process.env.OPENROUTER_API_KEY || "",
  };
  if (process.env.AI_MEDIA_KEYS_JSON) {
    try {
      const parsed = JSON.parse(process.env.AI_MEDIA_KEYS_JSON);
      if (!keys.googleStudioKey) {
        keys.googleStudioKey = parsed.GOOGLE_AI_STUDIO_API_KEY || parsed.GOOGLE_AQ_API_KEY || "";
      }
      if (!keys.openRouterKey && parsed.OPENROUTER_API_KEY) {
        keys.openRouterKey = parsed.OPENROUTER_API_KEY;
      }
    } catch {}
  }
  if (fs.existsSync(secretsPath)) {
    try {
      const raw = JSON.parse(fs.readFileSync(secretsPath, "utf8"));
      if (!keys.googleStudioKey) {
        keys.googleStudioKey = raw.GOOGLE_AI_STUDIO_API_KEY || raw.GOOGLE_AQ_API_KEY || "";
      }
      if (!keys.openRouterKey && raw.OPENROUTER_API_KEY) {
        keys.openRouterKey = raw.OPENROUTER_API_KEY;
      }
    } catch {}
  }
  return keys;
}

export function normalizeText(text) {
  return String(text || "").trim().replace(/\s+/g, " ").toLowerCase();
}

export function sha256Hash(text) {
  return crypto.createHash("sha256").update(normalizeText(text)).digest("hex");
}

export function escapeSqlString(text) {
  return String(text ?? "").replace(/'/g, "''");
}

export function isSpeculativeStatement(text) {
  return SPECULATIVE_REGEX.test(String(text || ""));
}

export function formatVectorLiteral(values) {
  const arr = Array.from(values, (v) => Number(Number(v || 0).toFixed(6)));
  return `[${arr.join(",")}]`;
}

/**
 * Deterministic zero-cost 384-dim normalized bag-of-ngrams embedding fallback for offline resilience.
 */
export function computeDeterministicEmbedding(text, dims = EMBEDDING_DIMS) {
  const vec = new Float64Array(dims);
  const tokens = normalizeText(text)
    .replace(/[^a-z0-9áéíóúâêôãõç_\- ]/gi, " ")
    .split(/\s+/)
    .filter((t) => t.length >= 2);

  for (const tok of tokens) {
    const h = crypto.createHash("sha256").update(tok).digest();
    const idx = ((h[0] << 8) | h[1]) % dims;
    const sign = (h[2] & 1) === 0 ? 1 : -1;
    vec[idx] += sign;
  }

  let norm = 0;
  for (let i = 0; i < dims; i++) norm += vec[i] * vec[i];
  norm = Math.sqrt(norm) || 1;
  const normalized = new Array(dims);
  for (let i = 0; i < dims; i++) normalized[i] = Number((vec[i] / norm).toFixed(5));
  return `[${normalized.join(",")}]`;
}

/**
 * Computes a real 384-d semantic embedding via Google AI Studio (gemini-embedding-001),
 * falling back cleanly to deterministic n-gram vectors when offline or rate-limited.
 */
export async function computeEmbedding(text, timeoutMs = 6000) {
  const clean = String(text || "").trim().slice(0, 6000);
  const { googleStudioKey } = loadAiKeys();
  if (googleStudioKey && clean) {
    try {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${NEURAL_EMBEDDING_MODEL}:embedContent?key=${googleStudioKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          signal: AbortSignal.timeout(timeoutMs),
          body: JSON.stringify({
            model: `models/${NEURAL_EMBEDDING_MODEL}`,
            content: { parts: [{ text: clean }] },
            outputDimensionality: EMBEDDING_DIMS,
          }),
        }
      );
      if (res.ok) {
        const data = await res.json();
        const vals = data?.embedding?.values;
        if (Array.isArray(vals) && vals.length === EMBEDDING_DIMS) {
          return {
            vectorLiteral: formatVectorLiteral(vals),
            model: NEURAL_EMBEDDING_MODEL,
          };
        }
      }
    } catch {}
  }

  return {
    vectorLiteral: computeDeterministicEmbedding(clean, EMBEDDING_DIMS),
    model: DETERMINISTIC_EMBEDDING_MODEL,
  };
}

export async function computeBatchEmbeddings(texts, timeoutMs = 15000) {
  const { googleStudioKey } = loadAiKeys();
  const cleanList = texts.map((t) => String(t || "").trim().slice(0, 6000) || "empty");
  if (!googleStudioKey || cleanList.length === 0) {
    return cleanList.map((t) => ({
      vectorLiteral: computeDeterministicEmbedding(t, EMBEDDING_DIMS),
      model: DETERMINISTIC_EMBEDDING_MODEL,
    }));
  }

  try {
    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${NEURAL_EMBEDDING_MODEL}:batchEmbedContents?key=${googleStudioKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        signal: AbortSignal.timeout(timeoutMs),
        body: JSON.stringify({
          requests: cleanList.map((t) => ({
            model: `models/${NEURAL_EMBEDDING_MODEL}`,
            content: { parts: [{ text: t }] },
            outputDimensionality: EMBEDDING_DIMS,
          })),
        }),
      }
    );
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data?.embeddings) && data.embeddings.length === cleanList.length) {
        return data.embeddings.map((e, idx) => {
          const vals = e?.values;
          if (Array.isArray(vals) && vals.length === EMBEDDING_DIMS) {
            return {
              vectorLiteral: formatVectorLiteral(vals),
              model: NEURAL_EMBEDDING_MODEL,
            };
          }
          return {
            vectorLiteral: computeDeterministicEmbedding(cleanList[idx], EMBEDDING_DIMS),
            model: DETERMINISTIC_EMBEDDING_MODEL,
          };
        });
      }
    }
  } catch {}

  const out = [];
  for (const t of cleanList) {
    out.push(await computeEmbedding(t));
  }
  return out;
}

export async function runSupabaseSql(query, timeoutMs = 8000) {
  const token = getSupabaseToken();
  for (let attempt = 0; attempt < 3; attempt++) {
    const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      signal: AbortSignal.timeout(timeoutMs),
      body: JSON.stringify({ query }),
    });
    if (res.status === 429 && attempt < 2) {
      await new Promise((r) => setTimeout(r, 700 * (attempt + 1)));
      continue;
    }
    if (!res.ok) {
      throw new Error(`Supabase SQL HTTP ${res.status}: ${await res.text()}`);
    }
    return res.json();
  }
}

export function classifyMemoryCategory(content, fallback = "episodic") {
  const txt = String(content || "");
  const errMatch = txt.match(/Errou:\s*([^|]+)\|\s*Corrigiu:\s*([^|]+)/i);
  const errPart = errMatch ? errMatch[1].trim() : "";
  const fixPart = errMatch ? errMatch[2].trim() : "";
  const hasRealErrorFix =
    Boolean(errMatch) &&
    !/^(zero|nenhum|nenhuma|sem\s+erro|n\/a|0\b)/i.test(errPart) &&
    !/^(nenhuma|nenhum|sem\s+corre|n\/a)/i.test(fixPart);

  if (
    hasRealErrorFix ||
    /\b(corre[çc][ãa]o cr[íi]tica|corre[çc][ãa]o do fundador|corre[çc][ãa]o definitiva|post-mortem|nunca misturar|regra cr[ií]tica)\b/i.test(
      txt
    )
  ) {
    return "correction";
  }
  if (/\b(treinamento|prefer[êe]ncia|regras fixas|perfil cognitivo|padrao visual|padr[ãa]o visual|sempre desenhar|nunca fazer)\b/i.test(txt)) {
    return "preference";
  }
  if (/\b(constitui[çc][ãa]o|estrat[ée]g|meta|50\/50|r\$\s*1\.000|plano evoluc|checklist vivo|port[ãa]o p0)\b/i.test(txt)) {
    return "strategic";
  }
  if (/\b(fix |ci |gradle|migration|script|deploy|build|workflow|api v3|sdk|r8|adb|keystore|hook)\b/i.test(txt)) {
    return "procedural";
  }
  return VALID_MEMORY_CATEGORIES.has(fallback) ? fallback : "episodic";
}

export async function saveValidatedMemory({
  content,
  origem = "cognitive_engine",
  categoria = "semantic",
  confianca = 0.9,
  supersedesId = null,
  allowSpeculative = false,
  metadata = {},
}) {
  const cleanContent = String(content || "").trim();
  if (!cleanContent || cleanContent.length < 6) {
    return { inserted: false, reason: "empty_or_too_short" };
  }
  if (!allowSpeculative && isSpeculativeStatement(cleanContent)) {
    return { inserted: false, reason: "rejected_speculative" };
  }

  const safeCategory = VALID_MEMORY_CATEGORIES.has(categoria) ? categoria : "semantic";
  const safeConf = Math.max(0, Math.min(1, Number(confianca) || 0.85));
  const hash = sha256Hash(cleanContent);

  const existing = await runSupabaseSql(
    `SELECT id, versao, categoria FROM public.memories WHERE content_hash = '${hash}' AND is_active = true LIMIT 1;`
  );
  if (Array.isArray(existing) && existing.length > 0) {
    return {
      inserted: false,
      deduplicated: true,
      existingId: existing[0].id,
      versao: existing[0].versao,
    };
  }

  let nextVersion = 1;
  let validSupersedesId = null;
  if (supersedesId) {
    const prevRows = await runSupabaseSql(
      `SELECT id, versao FROM public.memories WHERE id = ${Number(supersedesId)} LIMIT 1;`
    );
    if (Array.isArray(prevRows) && prevRows.length > 0) {
      validSupersedesId = Number(prevRows[0].id);
      nextVersion = Number(prevRows[0].versao || 1) + 1;
    }
  }

  const emb = await computeEmbedding(cleanContent);
  const safeMetaJson = escapeSqlString(JSON.stringify(metadata || {}));

  const insertedRows = await runSupabaseSql(`
    INSERT INTO public.memories (
      content, origem, categoria, confianca, versao, content_hash,
      is_active, embedding, embedding_model, metadata
    )
    VALUES (
      '${escapeSqlString(cleanContent)}',
      '${escapeSqlString(origem)}',
      '${safeCategory}',
      ${safeConf.toFixed(3)},
      ${nextVersion},
      '${hash}',
      true,
      '${emb.vectorLiteral}'::vector,
      '${escapeSqlString(emb.model)}',
      '${safeMetaJson}'::jsonb
    )
    RETURNING id, memory_uuid, versao, created_at;
  `);

  const created = insertedRows[0];
  if (validSupersedesId && created?.id) {
    await runSupabaseSql(`
      UPDATE public.memories
      SET is_active = false, superseded_by = ${Number(created.id)}
      WHERE id = ${validSupersedesId};
    `);
  }

  return {
    inserted: true,
    id: created.id,
    memory_uuid: created.memory_uuid,
    versao: created.versao,
    categoria: safeCategory,
    confianca: safeConf,
    embeddingModel: emb.model,
    supersededId: validSupersedesId,
  };
}

export async function retrieveRelevantContext({
  query = "",
  sessionId = null,
  limit = 8,
  minSim = 0.59,
  includeCore = true,
  includeDocs = true,
  includeConversations = true,
  includeTasksAndLearnings = true,
} = {}) {
  const safeLimit = Math.max(2, Math.min(20, Number(limit) || 8));
  const cleanQuery = String(query || "").trim();
  const safeMinSim = Math.max(0, Math.min(0.95, Number(minSim) || 0.59));

  try {
    let queryEmbeddingModel = null;
    let relevantSql = `
      SELECT id, content, origem, categoria, confianca, versao, created_at, embedding_model
      FROM public.memories
      WHERE is_active = true
        AND content !~ '\\[Ciclo #[0-9]+\\]'
      ORDER BY id DESC
      LIMIT ${safeLimit}
    `;

    if (cleanQuery) {
      const stopWords = new Set([
        "qual", "quais", "como", "para", "com", "que", "uma", "uns", "das", "dos",
        "nos", "nas", "responda", "frase", "curta", "atual", "sobre", "voce", "você",
        "ontem", "hoje", "agora", "isso", "este", "esta", "aqui", "onde", "quando",
      ]);
      const keywords = cleanQuery
        .replace(/[^a-zA-Z0-9áéíóúâêôãõç_\- ]/g, " ")
        .split(/\s+/)
        .map((w) => w.trim())
        .filter((w) => w.length >= 3 && !stopWords.has(w.toLowerCase()))
        .slice(0, 12);

      const tsQueryExpr =
        keywords.length > 0
          ? `websearch_to_tsquery('portuguese', '${escapeSqlString(keywords.join(" OR "))}')`
          : `plainto_tsquery('portuguese', '${escapeSqlString(cleanQuery)}')`;
      const exactTokens = keywords.filter((k) => /[A-Z0-9_]{4,}/.test(k)).slice(0, 3);
      const exactBoostSql =
        exactTokens.length > 0
          ? exactTokens
              .map((t) => `CASE WHEN content ILIKE '%${escapeSqlString(t)}%' THEN 5.0 ELSE 0.0 END`)
              .join(" + ")
          : "0.0";

      const qEmb = await computeEmbedding(cleanQuery);
      queryEmbeddingModel = qEmb.model;

      relevantSql = `
        SELECT
          h.id,
          h.content,
          h.origem,
          h.categoria,
          h.confianca,
          h.versao,
          h.created_at,
          h.embedding_model,
          h.fts_rank,
          h.vec_sim,
          h.rrf_score
        FROM public.hybrid_memory_search(
          '${escapeSqlString(cleanQuery)}',
          '${qEmb.vectorLiteral}'::vector,
          '${escapeSqlString(qEmb.model)}',
          ${safeLimit},
          60
        ) h
        WHERE h.fts_rank > 0 OR h.vec_sim >= ${safeMinSim} OR h.rrf_score >= 0.015
        ORDER BY h.rrf_score DESC, h.id DESC
        LIMIT ${safeLimit}
      `;
    }

    const coreSql = includeCore
      ? `SELECT coalesce(json_agg(t), '[]'::json) FROM (
           SELECT id, content, origem, categoria, confianca, versao, created_at
           FROM public.memories
           WHERE is_active = true
             AND categoria IN ('strategic', 'preference', 'correction', 'procedural')
             AND content !~ '\\[Ciclo #[0-9]+\\]'
           ORDER BY confianca DESC, id DESC
           LIMIT 5
         ) t`
      : `'[]'::json`;

    const convSql = includeConversations
      ? sessionId
        ? `SELECT coalesce(json_agg(t), '[]'::json) FROM (
             (SELECT id, question, answer, session_id, model_used, created_at
              FROM public.conversations
              WHERE session_id = '${escapeSqlString(sessionId)}'
              ORDER BY id DESC LIMIT 3)
             UNION ALL
             (SELECT id, question, answer, session_id, model_used, created_at
              FROM public.conversations
              WHERE session_id IS DISTINCT FROM '${escapeSqlString(sessionId)}'
              ORDER BY id DESC LIMIT 2)
           ) t`
        : `SELECT coalesce(json_agg(t), '[]'::json) FROM (
             SELECT id, question, answer, session_id, model_used, created_at
             FROM public.conversations
             ORDER BY id DESC
             LIMIT 4
           ) t`
      : `'[]'::json`;

    const tasksSql = includeTasksAndLearnings
      ? `SELECT coalesce(json_agg(t), '[]'::json) FROM (
           SELECT id, title, status, target, created_at
           FROM public.orchestrator_tasks
           WHERE status IN ('PENDING', 'RUNNING', 'IN_PROGRESS')
           ORDER BY id DESC
           LIMIT 5
         ) t`
      : `'[]'::json`;

    const learningsSql = includeTasksAndLearnings
      ? `SELECT coalesce(json_agg(t), '[]'::json) FROM (
           SELECT id, tipo, contexto, diagnostico, impacto_meta, created_at
           FROM public.aprendizado_ia
           ORDER BY id DESC
           LIMIT 3
         ) t`
      : `'[]'::json`;

    const docsSql = includeDocs
      ? `SELECT coalesce(json_agg(t), '[]'::json) FROM (
           SELECT id, left(content, 240) AS snippet, created_at
           FROM public.documents
           ORDER BY id DESC
           LIMIT 4
         ) t`
      : `'[]'::json`;

    const [bundle] = await runSupabaseSql(`
      SELECT json_build_object(
        'relevant', (SELECT coalesce(json_agg(r), '[]'::json) FROM (${relevantSql}) r),
        'core', (${coreSql}),
        'conversations', (${convSql}),
        'pendingTasks', (${tasksSql}),
        'learnings', (${learningsSql}),
        'docs', (${docsSql})
      ) AS payload;
    `);

    const p = bundle?.payload || {};
    const relevantMemories = Array.isArray(p.relevant) ? p.relevant : [];
    const coreMemories = Array.isArray(p.core) ? p.core : [];
    const conversations = Array.isArray(p.conversations) ? p.conversations : [];
    const pendingTasks = Array.isArray(p.pendingTasks) ? p.pendingTasks : [];
    const learnings = Array.isArray(p.learnings) ? p.learnings : [];
    const docs = Array.isArray(p.docs) ? p.docs : [];

    const seenIds = new Set();
    const memories = [];
    for (const row of [...relevantMemories, ...coreMemories]) {
      if (!seenIds.has(row.id)) {
        seenIds.add(row.id);
        memories.push(row);
      }
    }

    const lines = [];
    if (memories.length > 0) {
      lines.push("### MEMÓRIAS RELEVANTES & REGRAS ATIVAS");
      for (const m of memories) {
        const simLabel = m.vec_sim !== undefined ? ` | sim:${Number(m.vec_sim).toFixed(2)}` : "";
        lines.push(
          `- [#${m.id} | ${m.categoria} v${m.versao} | conf:${m.confianca}${simLabel} | ${String(m.created_at).slice(0, 10)}] ${String(m.content).replace(/\s+/g, " ").slice(0, 320)}`
        );
      }
    }
    if (conversations.length > 0) {
      lines.push("\n### HISTÓRICO RECENTE DE CONVERSAS");
      for (const c of conversations) {
        lines.push(
          `- [${c.model_used || "unknown"}] Q: ${String(c.question || "").replace(/\s+/g, " ").slice(0, 110)} | A: ${String(c.answer || "").replace(/\s+/g, " ").slice(0, 150)}`
        );
      }
    }
    if (docs.length > 0) {
      lines.push("\n### DOCUMENTOS HISTÓRICOS (Bloco de Notas / Pesquisas)");
      for (const d of docs) {
        lines.push(`- [Doc #${d.id}] ${String(d.snippet).replace(/\s+/g, " ")}`);
      }
    }
    if (learnings.length > 0) {
      lines.push("\n### APRENDIZADOS VALIDADOS (aprendizado_ia)");
      for (const l of learnings) {
        lines.push(`- [${l.tipo}] ${String(l.contexto).slice(0, 100)} -> ${String(l.diagnostico).slice(0, 160)}`);
      }
    }
    if (pendingTasks.length > 0) {
      lines.push("\n### TAREFAS PENDENTES");
      for (const t of pendingTasks) {
        lines.push(`- [Task #${t.id} | ${t.status}] ${t.title}`);
      }
    }

    return {
      source: "supabase_live",
      embeddingModel: queryEmbeddingModel,
      memories,
      coreMemories,
      conversations,
      pendingTasks,
      learnings,
      formattedContext: lines.join("\n"),
    };
  } catch (err) {
    const timelinePath = path.join(projectRoot, "docs", "LINHA_DO_TEMPO_AUTONOMA.md");
    const fallback = fs.existsSync(timelinePath) ? fs.readFileSync(timelinePath, "utf8").slice(-1800) : "";
    return {
      source: `offline_fallback (${err.message.slice(0, 80)})`,
      embeddingModel: null,
      memories: [],
      coreMemories: [],
      conversations: [],
      pendingTasks: [],
      learnings: [],
      formattedContext: fallback,
    };
  }
}

function parseJsonSafe(rawText) {
  const cleaned = String(rawText || "")
    .trim()
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/i, "")
    .trim();
  const s = cleaned.indexOf("{");
  const e = cleaned.lastIndexOf("}");
  if (s === -1 || e <= s) return null;
  try {
    return JSON.parse(cleaned.slice(s, e + 1));
  } catch {
    return null;
  }
}

function deterministicFallbackExtraction(question, answer) {
  const q = String(question || "").trim();
  const isCorrection =
    /\b(errado|corrija|corre[çc][ãa]o|n[ãa]o fa[çc]a|nunca mais|regra permanente|regra fixa|a partir de agora)\b/i.test(
      q
    );
  const facts = [];
  if (isCorrection && q.length >= 20 && !isSpeculativeStatement(q)) {
    return {
      should_learn: true,
      user_correction: { is_correction: true, content: `[Correção do Fundador] ${q.slice(0, 420)}`, supersedes_id: null },
      facts,
      extractor_source: "deterministic_fallback",
    };
  }
  const hasDurableDecision =
    /\b(decidido|aprovado|migration|implementado|corrigido|validado|passou|bloqueio|v\d+\.\d+\.\d+)\b/i.test(answer) &&
    answer.length >= 80;
  if (hasDurableDecision && !isSpeculativeStatement(answer.slice(0, 280))) {
    facts.push({
      content: `Resumo técnico (${q.slice(0, 90)}): ${answer.replace(/\s+/g, " ").slice(0, 260)}`,
      categoria: classifyMemoryCategory(`${q} ${answer}`, "semantic"),
      confianca: 0.85,
      supersedes_id: null,
    });
  }
  return {
    should_learn: facts.length > 0,
    user_correction: null,
    facts,
    extractor_source: "deterministic_fallback",
  };
}

export async function extractTurnKnowledgeWithLlm({ question, answer, candidateMemories = [] }) {
  const { googleStudioKey, openRouterKey } = loadAiKeys();
  const candidatesText = candidateMemories
    .slice(0, 6)
    .map((m) => `ID ${m.id} (${m.categoria} v${m.versao}): ${String(m.content).replace(/\s+/g, " ").slice(0, 200)}`)
    .join("\n");

  const extractorPrompt = `Analise o turno abaixo e extraia TODOS os conhecimentos duráveis, regras declaradas, parâmetros técnicos, decisões confirmadas, preferências permanentes ou correções explícitas do usuário — mesmo que sejam de experimento ou fora do app Android.
REGRAS ESTRITAS:
1. NUNCA invente e NUNCA registre afirmações especulativas ("talvez", "acho que", "suponho", "provavelmente", "hipótese").
2. Extraia qualquer regra explícita, protocolo ou conhecimento técnico informado pelo usuário para permanecer na memória entre sessões; só retorne should_learn=false se for conversa trivial/saudação ou fato já idêntico às memórias listadas abaixo.
3. Se um novo fato/correção contradizer ou atualizar uma memória listada abaixo, informe o ID numérico exato em "supersedes_id" (use APENAS IDs da lista abaixo ou null).
4. Categorias permitidas: "semantic", "procedural", "strategic", "preference", "correction", "episodic".
5. Responda SOMENTE em JSON válido no formato:
{"should_learn": boolean, "user_correction": {"is_correction": boolean, "content": string, "supersedes_id": number|null} | null, "facts": [{"content": string, "categoria": string, "confianca": number, "supersedes_id": number|null}]}

MEMÓRIAS EXISTENTES RELEVANTES:
${candidatesText || "(nenhuma)"}

PERGUNTA DO USUÁRIO:
${String(question || "").slice(0, 1200)}

RESPOSTA DO ASSISTENTE:
${String(answer || "").slice(0, 2200)}`;

  if (googleStudioKey) {
    for (const model of ["gemini-2.5-flash"]) {
      try {
        const res = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${googleStudioKey}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            signal: AbortSignal.timeout(6500),
            body: JSON.stringify({
              contents: [{ parts: [{ text: extractorPrompt }] }],
              generationConfig: { responseMimeType: "application/json", maxOutputTokens: 600 },
            }),
          }
        );
        if (res.ok) {
          const data = await res.json();
          const raw = (data?.candidates?.[0]?.content?.parts || []).map((p) => p.text || "").join("");
          const parsed = parseJsonSafe(raw);
          if (parsed && typeof parsed === "object") {
            return { ...parsed, extractor_source: `google_ai_studio/${model}` };
          }
        }
      } catch {}
    }
  }

  if (openRouterKey) {
    for (const model of ["openrouter/free", "nvidia/nemotron-3.5-lightning:free"]) {
      try {
        const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
          method: "POST",
          headers: { Authorization: `Bearer ${openRouterKey}`, "Content-Type": "application/json" },
          signal: AbortSignal.timeout(6500),
          body: JSON.stringify({
            model,
            max_tokens: 600,
            messages: [{ role: "user", content: extractorPrompt }],
          }),
        });
        if (res.ok) {
          const data = await res.json();
          const parsed = parseJsonSafe(data?.choices?.[0]?.message?.content || "");
          if (parsed && typeof parsed === "object") {
            return { ...parsed, extractor_source: model };
          }
        }
      } catch {}
    }
  }

  return deterministicFallbackExtraction(question, answer);
}

export async function learnFromInteraction({
  question,
  answer,
  modelUsed = "unknown",
  sessionId = null,
  userCorrection = null,
  extractedFacts = [],
  origem = "auto_learner",
  metadata = {},
}) {
  const safeMeta = escapeSqlString(JSON.stringify(metadata || {}));
  const convRows = await runSupabaseSql(`
    INSERT INTO public.conversations (question, answer, session_id, model_used, metadata)
    VALUES (
      '${escapeSqlString(String(question || "").slice(0, 4000))}',
      '${escapeSqlString(String(answer || "").slice(0, 8000))}',
      ${sessionId ? `'${escapeSqlString(sessionId)}'` : "NULL"},
      '${escapeSqlString(modelUsed)}',
      '${safeMeta}'::jsonb
    )
    RETURNING id;
  `);

  const savedMemories = [];

  if (userCorrection && String(userCorrection.content || "").trim()) {
    const corrRes = await saveValidatedMemory({
      content: userCorrection.content,
      origem: "user_correction",
      categoria: "correction",
      confianca: 0.99,
      supersedesId: userCorrection.supersedesId || null,
      metadata: { sessionId, modelUsed },
    });
    savedMemories.push(corrRes);
  }

  for (const fact of extractedFacts) {
    if (!fact || !fact.content) continue;
    const res = await saveValidatedMemory({
      content: fact.content,
      origem: fact.origem || origem,
      categoria: fact.categoria || "semantic",
      confianca: fact.confianca ?? 0.9,
      supersedesId: fact.supersedesId || null,
      metadata: { sessionId, modelUsed },
    });
    savedMemories.push(res);
  }

  return {
    conversationId: convRows[0]?.id || null,
    savedMemories,
  };
}

/**
 * Full post-turn continuous learning pipeline:
 * retrieves existing context -> extracts validated non-speculative facts & corrections ->
 * resolves contradictions -> persists conversation + memories with 384-d embeddings.
 */
export async function analyzeAndLearnTurn({
  question,
  answer,
  modelUsed = "unknown",
  sessionId = null,
  origem = "continuous_learner",
  skipLlmExtraction = false,
  userCorrection = null,
  extractedFacts = [],
  recordAprendizado = true,
}) {
  const cleanQ = String(question || "").trim();
  const cleanA = String(answer || "").trim();
  if (!cleanQ && !cleanA) {
    return { skipped: true, reason: "empty_turn" };
  }

  const isTrivial =
    TRIVIAL_TURN_REGEX.test(cleanQ) && cleanA.length < 180 && !userCorrection && extractedFacts.length === 0;
  if (isTrivial) {
    return { skipped: true, reason: "trivial_greeting" };
  }

  let finalCorrection = userCorrection;
  let finalFacts = Array.isArray(extractedFacts) ? [...extractedFacts] : [];
  let extractorSource = "explicit";

  if (!skipLlmExtraction && !finalCorrection && finalFacts.length === 0) {
    const ctx = await retrieveRelevantContext({
      query: `${cleanQ} ${cleanA.slice(0, 350)}`,
      sessionId,
      limit: 6,
    });
    const candidateIds = new Set((ctx.memories || []).map((m) => Number(m.id)));
    const extracted = await extractTurnKnowledgeWithLlm({
      question: cleanQ,
      answer: cleanA,
      candidateMemories: ctx.memories || [],
    });
    extractorSource = extracted.extractor_source || "llm_extractor";

    if (extracted.user_correction?.is_correction && extracted.user_correction.content) {
      const supId = Number(extracted.user_correction.supersedes_id);
      finalCorrection = {
        content: extracted.user_correction.content,
        supersedesId: candidateIds.has(supId) ? supId : null,
      };
    }

    if (Array.isArray(extracted.facts)) {
      for (const f of extracted.facts.slice(0, 4)) {
        if (!f?.content) continue;
        const supId = Number(f.supersedes_id);
        finalFacts.push({
          content: f.content,
          categoria: VALID_MEMORY_CATEGORIES.has(f.categoria) ? f.categoria : "semantic",
          confianca: Number(f.confianca) || 0.9,
          supersedesId: candidateIds.has(supId) ? supId : null,
          origem,
        });
      }
    }
  }

  const learned = await learnFromInteraction({
    question: cleanQ,
    answer: cleanA,
    modelUsed,
    sessionId,
    userCorrection: finalCorrection,
    extractedFacts: finalFacts,
    origem,
    metadata: { extractorSource },
  });

  if (recordAprendizado) {
    const significant = learned.savedMemories.filter(
      (m) => m.inserted && (m.categoria === "correction" || m.categoria === "strategic")
    );
    if (significant.length > 0) {
      try {
        const first = significant[0];
        const summaryText = (finalCorrection?.content || finalFacts[0]?.content || "").slice(0, 320);
        await runSupabaseSql(`
          INSERT INTO public.aprendizado_ia (tipo, contexto, diagnostico, peso_acao, impacto_meta, acao_executada, resultado)
          VALUES (
            '${first.categoria === "correction" ? "CORRECAO_USUARIO" : "ESTRATEGIA_COGNITIVA"}',
            '${escapeSqlString(cleanQ.slice(0, 180))}',
            '${escapeSqlString(summaryText)}',
            0.95,
            'Aprendizado continuo sem reincidencia de erros',
            'Extrator automatico (${escapeSqlString(extractorSource)}) -> Memory #${first.id}',
            'RESOLVIDO'
          );
        `);
      } catch {}
    }
  }

  return {
    skipped: false,
    extractorSource,
    ...learned,
  };
}

export async function backfillCognitiveMemories({
  batchSize = 25,
  pauseMs = 1200,
  reclassifyLegacy = true,
  reclassifyAllLegacy = false,
} = {}) {
  const whereClause = reclassifyAllLegacy
    ? `origem = 'legacy_session' OR embedding IS NULL OR embedding_model <> '${NEURAL_EMBEDDING_MODEL}' OR content_hash IS NULL`
    : `embedding IS NULL OR embedding_model <> '${NEURAL_EMBEDDING_MODEL}' OR content_hash IS NULL`;

  const rows = await runSupabaseSql(`
    SELECT id, content, origem, categoria, confianca, content_hash, embedding_model,
           (embedding IS NOT NULL) AS has_emb
    FROM public.memories
    WHERE ${whereClause}
    ORDER BY id ASC;
  `);

  if (!Array.isArray(rows) || rows.length === 0) {
    return { totalPending: 0, updated: 0, neuralEmbedded: 0, deterministicEmbedded: 0 };
  }

  let updated = 0;
  let neuralEmbedded = 0;
  let deterministicEmbedded = 0;

  for (let i = 0; i < rows.length; i += batchSize) {
    const chunk = rows.slice(i, i + batchSize);
    const needEmbIdx = [];
    const needEmbTexts = [];
    chunk.forEach((row, idx) => {
      if (!row.has_emb || row.embedding_model !== NEURAL_EMBEDDING_MODEL) {
        needEmbIdx.push(idx);
        needEmbTexts.push(row.content || "");
      }
    });

    const fetchedEmbs = needEmbTexts.length > 0 ? await computeBatchEmbeddings(needEmbTexts) : [];
    const embByLocalIdx = new Map();
    needEmbIdx.forEach((localIdx, k) => {
      embByLocalIdx.set(localIdx, fetchedEmbs[k]);
    });

    const statements = chunk.map((row, idx) => {
      const emb = embByLocalIdx.get(idx) || null;
      if (emb) {
        if (emb.model === NEURAL_EMBEDDING_MODEL) neuralEmbedded++;
        else deterministicEmbedded++;
      }

      const hash = row.content_hash || sha256Hash(row.content || "");
      let nextCat = row.categoria || "episodic";
      let nextConf = Number(row.confianca || 0.85);
      const shouldClassify =
        row.origem === "legacy_session" &&
        (reclassifyAllLegacy ? Number(row.id) !== 313 && Number(row.id) !== 315 : row.categoria === "episodic");
      if (reclassifyLegacy && shouldClassify) {
        nextCat = classifyMemoryCategory(row.content, "episodic");
        if (nextCat === "correction" || nextCat === "preference") nextConf = 0.95;
        else if (nextCat === "strategic") nextConf = 0.92;
        else if (nextCat === "procedural") nextConf = 0.88;
        else nextConf = 0.85;
      }

      const embUpdateSql = emb
        ? `, embedding = '${emb.vectorLiteral}'::vector, embedding_model = '${escapeSqlString(emb.model)}'`
        : "";

      return `
        UPDATE public.memories
        SET content_hash = '${escapeSqlString(hash)}',
            categoria = '${escapeSqlString(nextCat)}',
            confianca = ${nextConf.toFixed(3)}${embUpdateSql}
        WHERE id = ${Number(row.id)};
      `;
    });

    await runSupabaseSql(`BEGIN;\n${statements.join("\n")}\nCOMMIT;`, 20000);
    updated += chunk.length;
    if (needEmbTexts.length > 0 && i + batchSize < rows.length && pauseMs > 0) {
      await new Promise((r) => setTimeout(r, pauseMs));
    }
  }

  return {
    totalPending: rows.length,
    updated,
    neuralEmbedded,
    deterministicEmbedded,
  };
}

export async function computeCognitiveHealthMetrics() {
  const [memStats] = await runSupabaseSql(`
    SELECT
      count(*) AS total_memories,
      count(*) FILTER (WHERE is_active) AS active_memories,
      count(*) FILTER (WHERE NOT is_active AND superseded_by IS NOT NULL) AS superseded_memories,
      count(*) FILTER (WHERE embedding IS NOT NULL AND embedding_model = '${NEURAL_EMBEDDING_MODEL}') AS neural_embedded,
      count(*) FILTER (WHERE categoria = 'correction') AS corrections_count,
      count(*) FILTER (WHERE categoria IN ('strategic','preference','procedural','semantic')) AS structured_knowledge_count
    FROM public.memories;
  `);

  const [convStats] = await runSupabaseSql(`
    SELECT
      count(*) AS total_conversations,
      count(*) FILTER (WHERE session_id IS NOT NULL) AS session_tracked_conversations,
      count(DISTINCT model_used) FILTER (WHERE model_used IS NOT NULL) AS distinct_models
    FROM public.conversations;
  `);

  const evalRows = await runSupabaseSql(`
    SELECT id, created_at, model_used, suite_name, total_cases, passed_cases,
           retrieval_accuracy, validated_memories_count, corrected_responses_count,
           repeated_errors_count, cross_family_models
    FROM public.cognitive_evaluations
    ORDER BY id DESC
    LIMIT 5;
  `);

  const totalMem = Number(memStats?.total_memories || 0);
  const neuralCount = Number(memStats?.neural_embedded || 0);

  return {
    memories: {
      total: totalMem,
      active: Number(memStats?.active_memories || 0),
      superseded: Number(memStats?.superseded_memories || 0),
      neuralEmbedded: neuralCount,
      semanticCoveragePct: totalMem > 0 ? Number(((neuralCount / totalMem) * 100).toFixed(2)) : 0,
      correctionsCount: Number(memStats?.corrections_count || 0),
      structuredKnowledgeCount: Number(memStats?.structured_knowledge_count || 0),
    },
    conversations: {
      total: Number(convStats?.total_conversations || 0),
      sessionTracked: Number(convStats?.session_tracked_conversations || 0),
      distinctModels: Number(convStats?.distinct_models || 0),
    },
    recentEvaluations: evalRows || [],
  };
}

export async function recordEvaluation({
  modelUsed,
  suiteName,
  totalCases,
  passedCases,
  validatedMemoriesCount,
  correctedResponsesCount = 0,
  repeatedErrorsCount = 0,
  crossFamilyModels = [],
  details = {},
}) {
  const acc = totalCases > 0 ? passedCases / totalCases : 0;
  const modelsArrayLiteral =
    Array.isArray(crossFamilyModels) && crossFamilyModels.length > 0
      ? `ARRAY[${crossFamilyModels.map((m) => `'${escapeSqlString(m)}'`).join(",")}]::text[]`
      : `ARRAY[]::text[]`;

  const rows = await runSupabaseSql(`
    INSERT INTO public.cognitive_evaluations (
      model_used, suite_name, total_cases, passed_cases, retrieval_accuracy,
      validated_memories_count, corrected_responses_count, repeated_errors_count,
      cross_family_models, details_version, details
    )
    VALUES (
      '${escapeSqlString(modelUsed)}',
      '${escapeSqlString(suiteName)}',
      ${Number(totalCases)},
      ${Number(passedCases)},
      ${acc.toFixed(4)},
      ${Number(validatedMemoriesCount)},
      ${Number(correctedResponsesCount)},
      ${Number(repeatedErrorsCount)},
      ${modelsArrayLiteral},
      2,
      '${escapeSqlString(JSON.stringify(details))}'::jsonb
    )
    RETURNING id, created_at, retrieval_accuracy;
  `);
  return rows[0];
}
