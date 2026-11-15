import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..", "..");

export const VALID_MEMORY_CATEGORIES = new Set([
  "episodic",
  "semantic",
  "procedural",
  "strategic",
  "preference",
  "correction",
  "conversation",
]);

const SPECULATIVE_REGEX = /\b(talvez|acho que|suponho|provavelmente|chute|não verificado|sem confirmação|hipótese)\b/i;

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

/**
 * Deterministic zero-cost 384-dim normalized bag-of-ngrams embedding for pgvector.
 * Ensures semantic vector indexing works identically regardless of external LLM/API availability.
 */
export function computeDeterministicEmbedding(text, dims = 384) {
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
  const arr = new Array(dims);
  for (let i = 0; i < dims; i++) arr[i] = Number((vec[i] / norm).toFixed(5));
  return `[${arr.join(",")}]`;
}

export async function runSupabaseSql(query, timeoutMs = 6000) {
  const token = getSupabaseToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    signal: AbortSignal.timeout(timeoutMs),
    body: JSON.stringify({ query }),
  });
  if (!res.ok) {
    throw new Error(`Supabase SQL HTTP ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

export async function saveValidatedMemory({
  content,
  origem = "cognitive_engine",
  categoria = "semantic",
  confianca = 0.9,
  supersedesId = null,
  allowSpeculative = false,
}) {
  const cleanContent = String(content || "").trim();
  if (!cleanContent) {
    return { inserted: false, reason: "empty_content" };
  }
  if (!allowSpeculative && isSpeculativeStatement(cleanContent)) {
    return { inserted: false, reason: "rejected_speculative" };
  }
  const safeCategory = VALID_MEMORY_CATEGORIES.has(categoria) ? categoria : "semantic";
  const safeConf = Math.max(0, Math.min(1, Number(confianca) || 0.85));
  const hash = sha256Hash(cleanContent);

  // Deduplication check against active memories
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
  if (supersedesId) {
    const prevRows = await runSupabaseSql(
      `SELECT id, versao FROM public.memories WHERE id = ${Number(supersedesId)} LIMIT 1;`
    );
    if (Array.isArray(prevRows) && prevRows.length > 0) {
      nextVersion = Number(prevRows[0].versao || 1) + 1;
    }
  }

  const embLiteral = computeDeterministicEmbedding(cleanContent);
  const insertedRows = await runSupabaseSql(`
    INSERT INTO public.memories (content, origem, categoria, confianca, versao, content_hash, is_active, embedding)
    VALUES (
      '${escapeSqlString(cleanContent)}',
      '${escapeSqlString(origem)}',
      '${safeCategory}',
      ${safeConf.toFixed(3)},
      ${nextVersion},
      '${hash}',
      true,
      '${embLiteral}'::vector
    )
    RETURNING id, memory_uuid, versao, created_at;
  `);

  const created = insertedRows[0];
  if (supersedesId && created?.id) {
    await runSupabaseSql(`
      UPDATE public.memories
      SET is_active = false, superseded_by = ${Number(created.id)}
      WHERE id = ${Number(supersedesId)};
    `);
  }

  return {
    inserted: true,
    id: created.id,
    memory_uuid: created.memory_uuid,
    versao: created.versao,
    categoria: safeCategory,
    confianca: safeConf,
    supersededId: supersedesId || null,
  };
}

export async function retrieveRelevantContext({ query = "", sessionId = null, limit = 8 } = {}) {
  const safeLimit = Math.max(2, Math.min(20, Number(limit) || 8));
  const cleanQuery = String(query || "").trim();

  try {
    // 1. Core active strategic/preference/correction memories
    const coreMemories = await runSupabaseSql(`
      SELECT id, content, origem, categoria, confianca, versao, created_at
      FROM public.memories
      WHERE is_active = true
        AND categoria IN ('strategic', 'preference', 'correction', 'procedural')
      ORDER BY confianca DESC, id DESC
      LIMIT 4;
    `);

    // 2. Query-relevant memories via FTS + pgvector cosine similarity fallback
    let relevantMemories = [];
    if (cleanQuery) {
      const qEsc = escapeSqlString(cleanQuery);
      const qEmb = computeDeterministicEmbedding(cleanQuery);
      relevantMemories = await runSupabaseSql(`
        SELECT id, content, origem, categoria, confianca, versao, created_at,
               ts_rank(search_tsv, plainto_tsquery('portuguese', '${qEsc}')) AS fts_rank,
               CASE WHEN embedding IS NOT NULL THEN 1 - (embedding <=> '${qEmb}'::vector) ELSE 0 END AS vec_sim
        FROM public.memories
        WHERE is_active = true
        ORDER BY
          (ts_rank(search_tsv, plainto_tsquery('portuguese', '${qEsc}')) * 2.0 +
           CASE WHEN embedding IS NOT NULL THEN (1 - (embedding <=> '${qEmb}'::vector)) ELSE 0 END +
           (confianca::float * 0.2)) DESC,
          id DESC
        LIMIT ${safeLimit};
      `);
    } else {
      relevantMemories = await runSupabaseSql(`
        SELECT id, content, origem, categoria, confianca, versao, created_at
        FROM public.memories
        WHERE is_active = true
        ORDER BY id DESC
        LIMIT ${safeLimit};
      `);
    }

    // Merge & deduplicate by id
    const seenIds = new Set();
    const memories = [];
    for (const row of [...relevantMemories, ...coreMemories]) {
      if (!seenIds.has(row.id)) {
        seenIds.add(row.id);
        memories.push(row);
      }
    }

    // 3. Recent conversations (session + global)
    const sessionFilter = sessionId ? `WHERE session_id = '${escapeSqlString(sessionId)}'` : "";
    const conversations = await runSupabaseSql(`
      SELECT id, question, answer, session_id, model_used, created_at
      FROM public.conversations
      ${sessionFilter}
      ORDER BY id DESC
      LIMIT 4;
    `);

    // 4. Pending tasks & recent learnings
    const pendingTasks = await runSupabaseSql(`
      SELECT id, title, status, target_runner, created_at
      FROM public.orchestrator_tasks
      WHERE status IN ('PENDING', 'RUNNING')
      ORDER BY id DESC
      LIMIT 5;
    `);

    const learnings = await runSupabaseSql(`
      SELECT id, tipo, contexto, diagnostico, impacto_meta, created_at
      FROM public.aprendizado_ia
      ORDER BY id DESC
      LIMIT 3;
    `);

    const lines = [];
    if (memories.length > 0) {
      lines.push("### MEMÓRIAS RELEVANTES & REGRAS ATIVAS");
      for (const m of memories) {
        lines.push(`- [#${m.id} | ${m.categoria} v${m.versao} | conf:${m.confianca} | ${String(m.created_at).slice(0, 10)}] ${String(m.content).replace(/\s+/g, " ").slice(0, 320)}`);
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
    if (conversations.length > 0) {
      lines.push("\n### HISTÓRICO RECENTE DE CONVERSAS");
      for (const c of conversations) {
        lines.push(`- Q: ${String(c.question || "").slice(0, 120)} | A: ${String(c.answer || "").slice(0, 160)}`);
      }
    }

    return {
      source: "supabase_live",
      memories,
      conversations,
      pendingTasks,
      learnings,
      formattedContext: lines.join("\n"),
    };
  } catch (err) {
    // Offline fallback to local consolidated files
    const timelinePath = path.join(projectRoot, "docs", "LINHA_DO_TEMPO_AUTONOMA.md");
    const fallback = fs.existsSync(timelinePath) ? fs.readFileSync(timelinePath, "utf8").slice(-1600) : "";
    return {
      source: `offline_fallback (${err.message.slice(0, 80)})`,
      memories: [],
      conversations: [],
      pendingTasks: [],
      learnings: [],
      formattedContext: fallback,
    };
  }
}

export async function learnFromInteraction({
  question,
  answer,
  modelUsed = "unknown",
  sessionId = null,
  userCorrection = null,
  extractedFacts = [],
  origem = "auto_learner",
}) {
  // 1. Persist conversation turn
  const convRows = await runSupabaseSql(`
    INSERT INTO public.conversations (question, answer, session_id, model_used)
    VALUES (
      '${escapeSqlString(String(question || "").slice(0, 4000))}',
      '${escapeSqlString(String(answer || "").slice(0, 8000))}',
      ${sessionId ? `'${escapeSqlString(sessionId)}'` : "NULL"},
      '${escapeSqlString(modelUsed)}'
    )
    RETURNING id;
  `);

  const savedMemories = [];

  // 2. Persist explicit user correction if provided
  if (userCorrection && String(userCorrection.content || "").trim()) {
    const corrRes = await saveValidatedMemory({
      content: userCorrection.content,
      origem: "user_correction",
      categoria: "correction",
      confianca: 0.99,
      supersedesId: userCorrection.supersedesId || null,
    });
    savedMemories.push(corrRes);
  }

  // 3. Persist validated extracted facts (filtering out speculative ones & deduplicating)
  for (const fact of extractedFacts) {
    if (!fact || !fact.content) continue;
    const res = await saveValidatedMemory({
      content: fact.content,
      origem: fact.origem || origem,
      categoria: fact.categoria || "semantic",
      confianca: fact.confianca ?? 0.9,
      supersedesId: fact.supersedesId || null,
    });
    savedMemories.push(res);
  }

  return {
    conversationId: convRows[0]?.id || null,
    savedMemories,
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
  details = {},
}) {
  const acc = totalCases > 0 ? passedCases / totalCases : 0;
  const rows = await runSupabaseSql(`
    INSERT INTO public.cognitive_evaluations (
      model_used, suite_name, total_cases, passed_cases, retrieval_accuracy,
      validated_memories_count, corrected_responses_count, repeated_errors_count, details
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
      '${escapeSqlString(JSON.stringify(details))}'::jsonb
    )
    RETURNING id, created_at, retrieval_accuracy;
  `);
  return rows[0];
}
